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
public class PayablePaymentResponse {
    private Long id;
    private Long payableId;
    private BigDecimal monto;
    private String metodoPago;
    private String referenciaBancaria;
    private LocalDateTime fecha;
    private Long registradoPor;
}
