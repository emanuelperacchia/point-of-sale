package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionOrderResponse {

    private Long id;
    private Long recipeId;
    private String recipeNombre;
    private String productoTerminadoNombre;
    private Integer cantidadPlanificada;
    private Integer cantidadProducida;
    private LocalDate fechaPlanificada;
    private Long responsableId;
    private String responsableNombre;
    private Long sucursalId;
    private String estado;
    private String observaciones;
    private List<ProductionOrderComponentResponse> componentes;
    private String numeroLote;
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductionOrderComponentResponse {
        private Long id;
        private Long componenteId;
        private String componenteNombre;
        private BigDecimal cantidadPlanificada;
        private BigDecimal cantidadConsumida;
        private BigDecimal mermaReal;
        private String motivoMerma;
    }
}
