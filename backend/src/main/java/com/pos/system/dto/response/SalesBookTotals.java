package com.pos.system.dto.response;

import java.math.BigDecimal;

/**
 * Totales consolidados del libro de ventas.
 */
public record SalesBookTotals(
        BigDecimal totalNeto,
        BigDecimal totalIva,
        BigDecimal totalComprobantes,
        long cantidad
) {
    public static SalesBookTotals empty() {
        return new SalesBookTotals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
    }
}
