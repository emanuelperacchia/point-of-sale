package com.pos.system.dto.response;

import com.pos.system.entity.Absence;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbsenceResponse {

    private Long id;
    private Long employeeId;
    private LocalDate fecha;
    private Absence.Tipo tipo;
    private String descripcion;
    private Long aprobadoPor;
    private LocalDateTime createdAt;
}
