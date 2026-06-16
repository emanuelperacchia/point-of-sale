package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostAnalysisResponse {

    private Long productionOrderId;
    private Long recipeId;
    private String recipeNombre;
    private Integer cantidadProducida;
    private BigDecimal costoEstimadoTotal;
    private BigDecimal costoRealTotal;
    private BigDecimal desviacion;
    private BigDecimal costoUnitarioEstimado;
    private BigDecimal costoUnitarioReal;
    private List<CostDeviationItem> componentes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CostDeviationItem {
        private String componenteNombre;
        private BigDecimal cantidadPlanificada;
        private BigDecimal cantidadConsumida;
        private BigDecimal precioUnitario;
        private BigDecimal costoEstimado;
        private BigDecimal costoReal;
        private BigDecimal desviacion;
    }
}
