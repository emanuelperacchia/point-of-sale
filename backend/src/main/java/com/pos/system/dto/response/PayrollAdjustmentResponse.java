package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollAdjustmentResponse {
    private Long id;
    private Long payrollId;
    private String concepto;
    private BigDecimal monto;
    private String justificacion;
    private Long creadoPor;
    private LocalDateTime createdAt;
}
