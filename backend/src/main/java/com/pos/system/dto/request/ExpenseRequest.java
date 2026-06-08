package com.pos.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
        @NotNull @Positive BigDecimal monto,
        @NotNull LocalDate fecha,
        @NotBlank String categoria,
        Long proveedorId,
        @NotBlank String descripcion,
        Boolean recurrente,
        String frecuencia
) {
    public boolean isRecurrente() {
        return recurrente != null && recurrente;
    }
}
