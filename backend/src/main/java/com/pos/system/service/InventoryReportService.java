package com.pos.system.service;

import com.pos.system.dto.response.InventoryReportResponse;
import com.pos.system.dto.response.InventoryReportResponse.*;
import com.pos.system.repository.ProductRepository;
import com.pos.system.repository.ProductStockRepository;
import com.pos.system.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reportes de inventario con valorización y movimientos (US-038).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryReportService {

    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;
    private final StockMovementRepository stockMovementRepository;

    public InventoryReportResponse getInventoryReport() {
        try {
            Valorizacion val = buildValorizacion();

            List<StockPorProducto> stock = new ArrayList<>(); // simplificado
            List<ProductoPorVencer> porVencer = new ArrayList<>();
            List<MovimientoReciente> movs = buildMovimientosRecientes();

            return InventoryReportResponse.builder()
                    .valorizacion(val)
                    .stockPorProducto(stock)
                    .porVencer(porVencer)
                    .movimientosRecientes(movs)
                    .status("OK")
                    .build();
        } catch (Exception e) {
            log.warn("Error en reporte de inventario", e);
            return InventoryReportResponse.builder().status("ERROR").build();
        }
    }

    private Valorizacion buildValorizacion() {
        BigDecimal valorTotal = productStockRepository.getTotalInventoryValueGlobal();
        long totalProducts = productRepository.countByActiveTrue();
        long bajoStock = productStockRepository.countProductsBelowMinimum();
        return Valorizacion.builder()
                .valorTotal(valorTotal)
                .totalProducts(totalProducts)
                .bajoStock(bajoStock)
                .build();
    }

    private List<MovimientoReciente> buildMovimientosRecientes() {
        var movs = stockMovementRepository.findTop10ByOrderByCreatedAtDesc();
        List<MovimientoReciente> result = new ArrayList<>();
        for (var m : movs) {
            result.add(MovimientoReciente.builder()
                    .productId(m.getProduct().getId())
                    .productName(m.getProduct().getName())
                    .tipo(m.getType().name())
                    .cantidad(m.getQuantity().intValue())
                    .fecha(m.getCreatedAt().toLocalDate())
                    .referencia(m.getReason() != null ? m.getReason() : m.getReferenceDocument())
                    .build());
        }
        return result;
    }
}
