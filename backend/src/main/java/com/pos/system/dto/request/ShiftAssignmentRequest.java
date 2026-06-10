package com.pos.system.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftAssignmentRequest {

    @NotNull
    private Long employeeId;

    @NotNull
    private Long shiftDefinitionId;

    @NotNull
    private LocalDate semana;

    @NotNull
    private List<Integer> diasActivos; // List of day numbers (1=Monday, 7=Sunday)

    private Long sucursalId;
}
