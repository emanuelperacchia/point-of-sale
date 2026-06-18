package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Reportes de inventario (US-038).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReportResponse {

    private Valorizacion valorizacion;
    private List<StockPorProducto> stockPorProducto;
    private List<ProductoPorVencer> porVencer;
    private List<MovimientoReciente> movimientosRecientes;
    private String status;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Valorizacion {
        private BigDecimal valorTotal;
        private long totalProducts;
        private long bajoStock;
        private long sinStock;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StockPorProducto {
        private Long productId;
        private String productName;
        private String sku;
        private String warehouse;
        private Integer stockActual;
        private BigDecimal costoPromedio;
        private BigDecimal valorTotal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductoPorVencer {
        private Long productId;
        private String productName;
        private String lote;
        private LocalDate fechaVencimiento;
        private Integer cantidad;
        private Long diasParaVencer;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MovimientoReciente {
        private Long productId;
        private String productName;
        private String tipo;
        private Integer cantidad;
        private LocalDate fecha;
        private String referencia;
    }
}
