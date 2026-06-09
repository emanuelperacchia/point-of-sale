package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankStatementResponse {
    private Long id;
    private Long reconciliationId;
    private LocalDate fecha;
    private String descripcion;
    private BigDecimal monto;
    private String tipo;
    private String estado;
    private Long paymentId;
    private String observacion;
}
