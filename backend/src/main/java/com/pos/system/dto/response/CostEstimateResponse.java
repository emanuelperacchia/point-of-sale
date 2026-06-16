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
public class CostEstimateResponse {

    private Long recipeId;
    private String recipeNombre;
    private BigDecimal cantidadAProducir;
    private BigDecimal costoTotalEstimado;
    private BigDecimal costoUnitarioEstimado;
    private List<CostItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CostItem {
        private Long productoId;
        private String productoNombre;
        private BigDecimal cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal costoTotal;
    }
}
