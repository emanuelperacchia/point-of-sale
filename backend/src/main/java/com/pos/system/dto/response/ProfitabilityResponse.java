package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Análisis de rentabilidad (US-039).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfitabilityResponse {

    private MargenGeneral margenGeneral;
    private List<MargenPorProducto> porProducto;
    private PuntoEquilibrio puntoEquilibrio;
    private String status;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MargenGeneral {
        private BigDecimal ingresos;
        private BigDecimal costos;
        private BigDecimal gananciaBruta;
        private BigDecimal margenBrutoPct;
        private BigDecimal gastosOperativos;
        private BigDecimal gananciaNeta;
        private BigDecimal margenNetoPct;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MargenPorProducto {
        private Long productId;
        private String productName;
        private BigDecimal ventas;
        private BigDecimal costo;
        private BigDecimal ganancia;
        private BigDecimal margenPct;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PuntoEquilibrio {
        private BigDecimal gastosFijos;
        private BigDecimal margenContribucionPct;
        private BigDecimal puntoEquilibrioMonto;
        private BigDecimal puntoEquilibrioUnidades;
    }
}
