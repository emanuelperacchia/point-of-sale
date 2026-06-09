package com.pos.system.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayablePaymentRequest {
    @NotNull @DecimalMin("0.01")
    private BigDecimal monto;

    @NotBlank
    private String metodoPago;

    private String referenciaBancaria;
}
