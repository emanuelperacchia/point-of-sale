package com.pos.system.dto.request;

import com.pos.system.entity.Absence;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbsenceRequest {

    @NotNull
    private Long employeeId;

    @NotNull
    private LocalDate fecha;

    @NotNull
    private Absence.Tipo tipo;

    private String descripcion;
}
