package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftDefinitionResponse {

    private Long id;
    private String nombre;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private int diasSemana;
    private String color;
    private Boolean activo;
}
