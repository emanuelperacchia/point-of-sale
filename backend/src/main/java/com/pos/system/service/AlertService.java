package com.pos.system.service;

import com.pos.system.dto.response.AlertResponse;
import com.pos.system.dto.response.LowStockReportResponse;
import com.pos.system.entity.*;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final ProductStockRepository productStockRepository;
    private final UserRepository userRepository;

    /**
     * Obtiene alertas activas (no resueltas)
     */
    public Page<AlertResponse> getActiveAlerts(Pageable pageable) {
        return alertRepository.findByResolvedFalseOrderByCreatedAtDesc(pageable)
                .map(this::toAlertResponse);
    }

    /**
     * Obtiene alertas no leídas
     */
    public List<AlertResponse> getUnreadAlerts() {
        return alertRepository.findByResolvedFalseAndReadFalseOrderByCreatedAtDesc().stream()
                .map(this::toAlertResponse)
                .collect(Collectors.toList());
    }

    /**
     * Cuenta alertas activas
     */
    public long countActiveAlerts() {
        return alertRepository.countByResolvedFalse();
    }

    /**
     * Cuenta alertas no leídas
     */
    public long countUnreadAlerts() {
        return alertRepository.countByResolvedFalseAndReadFalse();
    }

    /**
     * Marca alerta como leída
     */
    @Transactional
    public AlertResponse markAsRead(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerta no encontrada: " + alertId));
        alert.setRead(true);
        return toAlertResponse(alertRepository.save(alert));
    }

    /**
     * Resuelve una alerta
     */
    @Transactional
    public AlertResponse resolveAlert(Long alertId, Long userId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerta no encontrada: " + alertId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userId));

        alert.setResolved(true);
        alert.setResolvedAt(LocalDateTime.now());
        alert.setResolvedBy(user);
        return toAlertResponse(alertRepository.save(alert));
    }

    /**
     * Genera alertas para todos los productos bajo stock mínimo
     * Llamado por el scheduler
     */
    @Transactional
    public int generateLowStockAlerts() {
        List<ProductStock> lowStockProducts = productStockRepository.findProductsBelowMinimum();
        int count = 0;

        for (ProductStock ps : lowStockProducts) {
            // No crear alerta duplicada si ya existe una activa para este producto/bodega
            List<Alert> existingAlerts = alertRepository.findActiveAlertsByProduct(ps.getProduct().getId());
            boolean hasActiveAlert = existingAlerts.stream()
                    .anyMatch(a -> a.getWarehouse().getId().equals(ps.getWarehouse().getId()));

            if (!hasActiveAlert) {
                Alert.AlertType type;
                Alert.AlertSeverity severity;

                if (ps.getCurrentStock().compareTo(BigDecimal.ZERO) == 0) {
                    type = Alert.AlertType.STOCK_AGOTADO;
                    severity = Alert.AlertSeverity.CRITICAL;
                } else if (ps.getCurrentStock().compareTo(ps.getMinimumStock().multiply(BigDecimal.valueOf(0.5))) < 0) {
                    type = Alert.AlertType.STOCK_CRITICO;
                    severity = Alert.AlertSeverity.HIGH;
                } else {
                    type = Alert.AlertType.STOCK_BAJO;
                    severity = Alert.AlertSeverity.MEDIUM;
                }

                Alert alert = Alert.builder()
                        .product(ps.getProduct())
                        .warehouse(ps.getWarehouse())
                        .type(type)
                        .severity(severity)
                        .message(generateMessage(ps, type))
                        .currentStock(ps.getCurrentStock())
                        .minimumStock(ps.getMinimumStock())
                        .build();

                alertRepository.save(alert);
                count++;
            }
        }

        if (count > 0) {
            log.info("Generadas {} alertas de stock bajo", count);
        }

        return count;
    }

    /**
     * Genera reporte de productos bajo stock mínimo
     */
    public LowStockReportResponse getLowStockReport() {
        List<ProductStock> lowStockProducts = productStockRepository.findProductsBelowMinimum();

        List<LowStockReportResponse.LowStockItem> items = lowStockProducts.stream()
                .map(ps -> LowStockReportResponse.LowStockItem.builder()
                        .productId(ps.getProduct().getId())
                        .productName(ps.getProduct().getName())
                        .productSku(ps.getProduct().getSku())
                        .warehouseId(ps.getWarehouse().getId())
                        .warehouseName(ps.getWarehouse().getName())
                        .currentStock(ps.getCurrentStock())
                        .minimumStock(ps.getMinimumStock())
                        .suggestedPurchase(ps.getSuggestedPurchaseQuantity())
                        .build())
                .collect(Collectors.toList());

        return LowStockReportResponse.builder()
                .totalProductsBelowMinimum((long) items.size())
                .items(items)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private String generateMessage(ProductStock ps, Alert.AlertType type) {
        return switch (type) {
            case STOCK_AGOTADO -> String.format(
                    "El producto %s está AGOTADO en %s", ps.getProduct().getName(), ps.getWarehouse().getName());
            case STOCK_CRITICO -> String.format(
                    "El producto %s tiene stock CRÍTICO en %s. Actual: %.2f, Mínimo: %.2f",
                    ps.getProduct().getName(), ps.getWarehouse().getName(),
                    ps.getCurrentStock(), ps.getMinimumStock());
            case STOCK_BAJO -> String.format(
                    "El producto %s está por debajo del mínimo en %s. Actual: %.2f, Mínimo: %.2f",
                    ps.getProduct().getName(), ps.getWarehouse().getName(),
                    ps.getCurrentStock(), ps.getMinimumStock());
            default -> "Alerta de stock para " + ps.getProduct().getName();
        };
    }

    private AlertResponse toAlertResponse(Alert alert) {
        return AlertResponse.builder()
                .id(alert.getId())
                .productId(alert.getProduct().getId())
                .productName(alert.getProduct().getName())
                .productSku(alert.getProduct().getSku())
                .warehouseId(alert.getWarehouse().getId())
                .warehouseName(alert.getWarehouse().getName())
                .type(alert.getType().name())
                .severity(alert.getSeverity().name())
                .message(alert.getMessage())
                .currentStock(alert.getCurrentStock())
                .minimumStock(alert.getMinimumStock())
                .read(alert.getRead())
                .resolved(alert.getResolved())
                .resolvedAt(alert.getResolvedAt())
                .resolvedBy(alert.getResolvedBy() != null ? alert.getResolvedBy().getFullName() : null)
                .createdAt(alert.getCreatedAt())
                .build();
    }
}