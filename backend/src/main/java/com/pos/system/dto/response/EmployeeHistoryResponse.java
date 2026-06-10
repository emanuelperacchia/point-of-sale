package com.pos.system.dto.response;

import com.pos.system.entity.EmployeeHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeHistoryResponse {

    private Long id;
    private Long employeeId;
    private EmployeeHistory.Campo campo;
    private String valorAnterior;
    private String valorNuevo;
    private LocalDateTime fecha;
    private Long modificadoPor;
}
