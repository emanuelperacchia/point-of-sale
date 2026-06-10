package com.pos.system.dto.response;

import com.pos.system.entity.AttendanceRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceResponse {

    private Long id;
    private Long employeeId;
    private LocalDate fecha;
    private LocalTime horaEntrada;
    private LocalTime horaSalida;
    private Integer horasTrabajadasMinutos;
    private Integer horasExtraMinutos;
    private AttendanceRecord.Estado estado;
    private String observacion;
    private LocalDateTime createdAt;
}
