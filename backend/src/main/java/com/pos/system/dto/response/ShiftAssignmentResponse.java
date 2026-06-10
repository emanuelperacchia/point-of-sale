package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftAssignmentResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long shiftDefinitionId;
    private String shiftName;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private LocalDate semana;
    private List<Integer> diasActivos;
    private Long sucursalId;
}
