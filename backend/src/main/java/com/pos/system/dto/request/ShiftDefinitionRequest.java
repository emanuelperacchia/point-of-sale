package com.pos.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftDefinitionRequest {

    @NotBlank
    private String nombre;

    @NotNull
    private LocalTime horaInicio;

    @NotNull
    private LocalTime horaFin;

    private int diasSemana;

    private String color;
}
