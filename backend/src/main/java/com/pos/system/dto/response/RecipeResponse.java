package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private Long productoTerminadoId;
    private String productoTerminadoNombre;
    private BigDecimal cantidadProducida;
    private String unidadMedida;
    private Integer tiempoProduccionMinutos;
    private Boolean activa;
    private BigDecimal costoEstimado;
    private List<BomComponentResponse> componentes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BomComponentResponse {
        private Long id;
        private Long componenteId;
        private String componenteNombre;
        private String componenteSku;
        private String componenteTipo;
        private BigDecimal cantidad;
        private String unidadMedida;
        private Boolean esMermaEsperada;
        private BigDecimal porcentajeMermaEsperado;
    }
}
