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
public class BomExplosionResponse {

    private Long recipeId;
    private String recipeNombre;
    private BigDecimal cantidadAProducir;
    private List<BomExplosionItem> materiales;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BomExplosionItem {
        private Long productoId;
        private String productoNombre;
        private String productoSku;
        private String tipo;
        private BigDecimal cantidadTotal;
        private String unidadMedida;
        private BigDecimal precioPromedio;
        private BigDecimal costoTotal;
        private Boolean stockSuficiente;
        private Integer stockActual;
        private Integer stockFaltante;
    }
}
