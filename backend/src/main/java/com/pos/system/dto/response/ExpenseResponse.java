package com.pos.system.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseResponse(
        Long id,
        BigDecimal monto,
        LocalDate fecha,
        String categoria,
        Long proveedorId,
        String descripcion,
        String estado,
        String comprobanteUrl,
        boolean recurrente,
        String frecuencia,
        LocalDate proximaFecha
) {
    public boolean tieneComprobante() {
        return comprobanteUrl != null && !comprobanteUrl.isBlank();
    }
}
