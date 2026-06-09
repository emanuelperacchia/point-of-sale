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
public class ReceivableResponse {
    private Long id;
    private Long clientId;
    private String clientName;
    private String clientDocument;
    private Long saleId;
    private BigDecimal montoOriginal;
    private BigDecimal saldoPendiente;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private String estado;
    private BigDecimal interesesAcumulados;
}
