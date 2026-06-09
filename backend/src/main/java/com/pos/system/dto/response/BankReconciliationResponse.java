package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankReconciliationResponse {
    private Long id;
    private String periodo;
    private BigDecimal totalExtracto;
    private BigDecimal totalSistema;
    private BigDecimal diferencia;
    private String estado;
    private long totalLineas;
    private long conciliadas;
    private long pendientes;
}
