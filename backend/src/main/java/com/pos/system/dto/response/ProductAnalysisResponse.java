package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Análisis de productos con clasificación ABC y rotación (US-037).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAnalysisResponse {

    private List<ProductoABC> clasificacionABC;
    private ResumenABC resumen;
    private List<ProductoSinMovimiento> sinMovimiento;
    private String status;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductoABC {
        private Long productId;
        private String productName;
        private String productSku;
        private String categoria;
        private BigDecimal ventas;
        private Long cantidad;
        private BigDecimal porcentajeAcumulado;
        private String clasificacion; // "A", "B", "C"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResumenABC {
        private Long totalProductos;
        private Long productosClaseA;
        private Long productosClaseB;
        private Long productosClaseC;
        private BigDecimal ventasClaseA;
        private BigDecimal ventasClaseB;
        private BigDecimal ventasClaseC;
        private BigDecimal porcentajeACobertura;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductoSinMovimiento {
        private Long productId;
        private String productName;
        private String productSku;
        private Integer stockActual;
        private BigDecimal costoPromedio;
        private Long diasSinVenta;
    }
}
