package com.pos.system.dto.response;

import com.pos.system.entity.ShiftChangeRequest;
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
public class ShiftChangeRequestResponse {

    private Long id;
    private Long employeeId;
    private Long assignmentId;
    private LocalDate fechaOriginal;
    private String motivo;
    private ShiftChangeRequest.Estado estado;
    private Long revisadoPor;
    private LocalDateTime fechaRevision;
    private LocalDateTime createdAt;
}
