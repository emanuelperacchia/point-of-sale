package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceSummaryResponse {

    private Long employeeId;
    private int mes;
    private int anio;
    private int diasTrabajados;
    private int horasTotalesMinutos;
    private int horasExtraMinutos;
    private int ausenciasJustificadas;
    private int ausenciasInjustificadas;
    private int licencias;
    private int vacaciones;
}
