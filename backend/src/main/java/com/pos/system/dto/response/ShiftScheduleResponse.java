package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Weekly schedule grid: employeeId → dayOfWeek → shift assignments for that day.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftScheduleResponse {

    private LocalDate semana;
    private Long sucursalId;
    private List<EmployeeSchedule> empleados;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EmployeeSchedule {
        private Long employeeId;
        private String employeeName;
        /** Map of dayOfWeek (1=Monday) → list of shift info for that day */
        private Map<Integer, List<ShiftInfo>> turnos;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShiftInfo {
        private Long assignmentId;
        private Long shiftDefinitionId;
        private String shiftName;
        private String horaInicio;
        private String horaFin;
        private String color;
    }
}
