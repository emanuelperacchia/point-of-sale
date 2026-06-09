package com.pos.system.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class PayableRequest {
    @NotNull
    private Long supplierId;

    private Long purchaseOrderId;

    @NotNull @DecimalMin("0.01")
    private BigDecimal montoOriginal;

    @NotNull
    private LocalDate fechaEmision;

    @NotNull
    private LocalDate fechaVencimiento;

    private String referenciaBancaria;
}
