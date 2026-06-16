package com.pos.system.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProductionOrderRequest {

    @NotNull
    private Long recipeId;

    @NotNull
    @Min(1)
    private Integer cantidadPlanificada;

    @NotNull
    private LocalDate fechaPlanificada;

    @NotNull
    private Long responsableId;

    private Long sucursalId;

    private String observaciones;
}
