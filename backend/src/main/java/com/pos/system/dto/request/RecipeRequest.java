package com.pos.system.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RecipeRequest {

    @NotBlank
    @Size(max = 200)
    private String nombre;

    @Size(max = 2000)
    private String descripcion;

    @NotNull
    private Long productoTerminadoId;

    @NotNull
    private BigDecimal cantidadProducida;

    @NotBlank
    @Size(max = 20)
    private String unidadMedida;

    private Integer tiempoProduccionMinutos;

    private List<BomComponentRequest> componentes;

    @Data
    public static class BomComponentRequest {
        @NotNull
        private Long componenteId;

        @NotNull
        private BigDecimal cantidad;

        @NotBlank
        @Size(max = 20)
        private String unidadMedida;

        private Boolean esMermaEsperada;

        private BigDecimal porcentajeMermaEsperado;
    }
}
