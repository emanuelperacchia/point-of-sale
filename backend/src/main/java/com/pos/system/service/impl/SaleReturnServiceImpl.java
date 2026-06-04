package com.pos.system.service.impl;

import com.pos.system.dto.request.CreateReturnRequest;
import com.pos.system.dto.request.StockMovementRequest;
import com.pos.system.dto.response.ReturnItemResponse;
import com.pos.system.dto.response.SaleReturnResponse;
import com.pos.system.entity.*;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.*;
import com.pos.system.service.SaleReturnService;
import com.pos.system.service.StockMovementService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleReturnServiceImpl implements SaleReturnService {

    private static final Logger log = LoggerFactory.getLogger(SaleReturnServiceImpl.class);

    private final SaleReturnRepository saleReturnRepository;
    private final ReturnItemRepository returnItemRepository;
    private final SaleRepository saleRepository;
    private final StockMovementService stockMovementService;
    private final UserRepository userRepository;

    @Value("${returns.auto-approve-limit:5000}")
    private BigDecimal autoApproveLimit;

    @Override
    @Transactional
    public SaleReturnResponse createReturn(CreateReturnRequest request, Long usuarioId) {
        // Validar venta
        Sale sale = saleRepository.findById(request.getSaleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Venta no encontrada: " + request.getSaleId()));

        // Validar items contra la venta original
        List<SaleItem> saleItems = sale.getItems();
        Map<Long, SaleItem> saleItemMap = saleItems.stream()
                .collect(Collectors.toMap(SaleItem::getId, si -> si));

        // Calcular cantidades ya devueltas
        Map<Long, Integer> alreadyReturned = calculateAlreadyReturned(request.getSaleId());

        // Determinar método de devolución (coincide con método de pago original)
        List<Payment> payments = sale.getPayments();
        PaymentMethod metodoDevolucion = determineMetodoDevolucion(payments);

        // Construir items de la devolución y calcular monto total
        List<ReturnItem> returnItems = new ArrayList<>();
        BigDecimal montoTotal = BigDecimal.ZERO;

        for (var itemReq : request.getItems()) {
            SaleItem originalItem = saleItemMap.get(itemReq.getSaleItemId());
            if (originalItem == null) {
                throw new BadRequestException(
                        "Item " + itemReq.getSaleItemId() + " no pertenece a la venta " + request.getSaleId());
            }

            // Validar cantidad
            int alreadyReturnedQty = alreadyReturned.getOrDefault(itemReq.getSaleItemId(), 0);
            int maxReturnable = originalItem.getQuantity() - alreadyReturnedQty;

            if (itemReq.getCantidad() <= 0) {
                throw new BadRequestException("La cantidad debe ser mayor a 0");
            }
            if (itemReq.getCantidad() > maxReturnable) {
                throw new BadRequestException(
                        "Cantidad máxima a devolver para el item " + itemReq.getSaleItemId()
                        + ": " + maxReturnable + " (ya devueltos: " + alreadyReturnedQty + ")");
            }

            BigDecimal precioUnitario = originalItem.getUnitPrice();
            BigDecimal itemSubtotal = precioUnitario.multiply(BigDecimal.valueOf(itemReq.getCantidad()));

            returnItems.add(ReturnItem.builder()
                    .saleItemId(itemReq.getSaleItemId())
                    .cantidad(itemReq.getCantidad())
                    .precioUnitario(precioUnitario)
                    .build());

            montoTotal = montoTotal.add(itemSubtotal);
        }

        // Determinar si auto-aprueba o requiere aprobación
        boolean autoApprove = montoTotal.compareTo(autoApproveLimit) <= 0;
        ReturnStatus estado = autoApprove ? ReturnStatus.APROBADA : ReturnStatus.PENDIENTE_APROBACION;

        // Crear la devolución
        SaleReturn saleReturn = SaleReturn.builder()
                .saleId(request.getSaleId())
                .estado(estado)
                .motivo(request.getMotivo())
                .montoTotal(montoTotal)
                .metodoDevolucion(metodoDevolucion)
                .build();

        if (autoApprove) {
            saleReturn.setAprobadorId(usuarioId);
        }

        saleReturn = saleReturnRepository.save(saleReturn);

        // Persistir items de la devolución
        final Long returnId = saleReturn.getId();
        for (ReturnItem ri : returnItems) {
            ri.setReturnId(returnId);
        }
        returnItemRepository.saveAll(returnItems);

        // Si auto-aprueba, reintegrar stock inmediatamente
        if (autoApprove) {
            reintegrateStock(saleReturn, returnItems, sale, usuarioId);
            log.info("Devolución auto-aprobada: ID={}, venta={}, monto={}",
                    returnId, request.getSaleId(), montoTotal);
        } else {
            log.info("Devolución pendiente de aprobación: ID={}, venta={}, monto={}",
                    returnId, request.getSaleId(), montoTotal);
        }

        return mapToResponse(saleReturn, returnItems);
    }

    @Override
    @Transactional
    public SaleReturnResponse approveReturn(Long returnId, Long aprobadorId) {
        SaleReturn saleReturn = saleReturnRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Devolución no encontrada: " + returnId));

        if (saleReturn.getEstado() != ReturnStatus.PENDIENTE_APROBACION) {
            throw new BadRequestException(
                    "Solo se pueden aprobar devoluciones pendientes. Estado actual: " + saleReturn.getEstado());
        }

        // Verificar aprobador
        userRepository.findById(aprobadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + aprobadorId));

        // Reintegrar stock
        List<ReturnItem> items = returnItemRepository.findByReturnId(returnId);
        Long saleId = saleReturn.getSaleId();
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Venta no encontrada: " + saleId));

        reintegrateStock(saleReturn, items, sale, aprobadorId);

        // Actualizar estado
        saleReturn.setEstado(ReturnStatus.APROBADA);
        saleReturn.setAprobadorId(aprobadorId);
        saleReturn.setUpdatedAt(LocalDateTime.now());
        SaleReturn saved = saleReturnRepository.save(saleReturn);

        log.info("Devolución aprobada: ID={}, aprobador={}", returnId, aprobadorId);

        return mapToResponse(saved, items);
    }

    @Override
    @Transactional
    public SaleReturnResponse rejectReturn(Long returnId, Long aprobadorId) {
        SaleReturn saleReturn = saleReturnRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Devolución no encontrada: " + returnId));

        if (saleReturn.getEstado() != ReturnStatus.PENDIENTE_APROBACION) {
            throw new BadRequestException(
                    "Solo se pueden rechazar devoluciones pendientes. Estado actual: " + saleReturn.getEstado());
        }

        saleReturn.setEstado(ReturnStatus.RECHAZADA);
        saleReturn.setAprobadorId(aprobadorId);
        saleReturn.setUpdatedAt(LocalDateTime.now());
        saleReturn = saleReturnRepository.save(saleReturn);

        List<ReturnItem> items = returnItemRepository.findByReturnId(returnId);
        log.info("Devolución rechazada: ID={}, aprobador={}", returnId, aprobadorId);

        return mapToResponse(saleReturn, items);
    }

    @Override
    @Transactional(readOnly = true)
    public SaleReturnResponse getById(Long id) {
        SaleReturn saleReturn = saleReturnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Devolución no encontrada: " + id));
        List<ReturnItem> items = returnItemRepository.findByReturnId(id);
        return mapToResponse(saleReturn, items);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaleReturnResponse> findBySaleId(Long saleId) {
        return saleReturnRepository.findBySaleIdOrderByCreatedAtDesc(saleId)
                .stream()
                .map(sr -> {
                    List<ReturnItem> items = returnItemRepository.findByReturnId(sr.getId());
                    return mapToResponse(sr, items);
                })
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private void reintegrateStock(SaleReturn saleReturn, List<ReturnItem> items,
                                  Sale sale, Long usuarioId) {
        User user = userRepository.findById(usuarioId)
                .orElse(null); // si no se encuentra, continuamos sin creador

        Warehouse warehouse = sale.getWarehouse();

        for (ReturnItem item : items) {
            // Buscar el SaleItem original para obtener el productId
            SaleItem originalItem = sale.getItems().stream()
                    .filter(si -> si.getId().equals(item.getSaleItemId()))
                    .findFirst()
                    .orElse(null);

            if (originalItem == null || originalItem.getProduct() == null) {
                log.warn("Producto no encontrado para returnItem {}, saltando reintegro", item.getId());
                continue;
            }

            StockMovementRequest movementReq = new StockMovementRequest();
            movementReq.setProductId(originalItem.getProduct().getId());
            movementReq.setWarehouseId(warehouse.getId());
            movementReq.setQuantity(BigDecimal.valueOf(item.getCantidad()));
            movementReq.setReason("Devolución #" + saleReturn.getId() + " - Venta #" + sale.getId());
            movementReq.setReferenceDocument("RETURN-" + saleReturn.getId());

            stockMovementService.entryByReturn(movementReq, user);
        }
    }

    /**
     * Calcula las cantidades ya devueltas por saleItemId.
     */
    private Map<Long, Integer> calculateAlreadyReturned(Long saleId) {
        List<SaleReturn> previousReturns = saleReturnRepository.findBySaleIdOrderByCreatedAtDesc(saleId);

        return previousReturns.stream()
                .filter(sr -> sr.getEstado() == ReturnStatus.APROBADA)
                .flatMap(sr -> returnItemRepository.findByReturnId(sr.getId()).stream())
                .collect(Collectors.groupingBy(
                        ReturnItem::getSaleItemId,
                        Collectors.summingInt(ReturnItem::getCantidad)
                ));
    }

    /**
     * Determina el método de devolución basado en los pagos originales.
     * Si hay un solo método, se usa ese. Si hay múltiples, se usa el que tenga mayor monto.
     */
    private PaymentMethod determineMetodoDevolucion(List<Payment> payments) {
        if (payments == null || payments.isEmpty()) {
            return PaymentMethod.CASH;
        }
        if (payments.size() == 1) {
            return payments.get(0).getPaymentMethod();
        }
        // Múltiples pagos — usar el de mayor monto
        return payments.stream()
                .max((a, b) -> a.getAmount().compareTo(b.getAmount()))
                .map(Payment::getPaymentMethod)
                .orElse(PaymentMethod.CASH);
    }

    private SaleReturnResponse mapToResponse(SaleReturn saleReturn, List<ReturnItem> items) {
        List<ReturnItemResponse> itemResponses = items.stream()
                .map(ri -> ReturnItemResponse.builder()
                        .id(ri.getId())
                        .saleItemId(ri.getSaleItemId())
                        .cantidad(ri.getCantidad())
                        .precioUnitario(ri.getPrecioUnitario())
                        .subtotal(ri.getPrecioUnitario()
                                .multiply(BigDecimal.valueOf(ri.getCantidad())))
                        .build())
                .toList();

        return SaleReturnResponse.builder()
                .id(saleReturn.getId())
                .saleId(saleReturn.getSaleId())
                .estado(saleReturn.getEstado())
                .motivo(saleReturn.getMotivo())
                .aprobadorId(saleReturn.getAprobadorId())
                .montoTotal(saleReturn.getMontoTotal())
                .metodoDevolucion(saleReturn.getMetodoDevolucion())
                .notaCreditoId(saleReturn.getNotaCreditoId())
                .referenciaDevolucion(saleReturn.getReferenciaDevolucion())
                .items(itemResponses)
                .createdAt(saleReturn.getCreatedAt())
                .build();
    }
}
