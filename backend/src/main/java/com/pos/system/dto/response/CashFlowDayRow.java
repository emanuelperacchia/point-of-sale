package com.pos.system.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Un día del flujo de caja.
 */
public record CashFlowDayRow(
        LocalDate fecha,
        BigDecimal ingresos,
        BigDecimal egresos,
        BigDecimal saldoDia,
        BigDecimal saldoAcumulado,
        boolean esProyectado
) {
    public static CashFlowDayRow real(LocalDate fecha, BigDecimal ingresos, BigDecimal egresos, BigDecimal saldoAcumulado) {
        BigDecimal saldoDia = ingresos.subtract(egresos);
        return new CashFlowDayRow(fecha, ingresos, egresos, saldoDia, saldoAcumulado, false);
    }

    public static CashFlowDayRow proyectado(LocalDate fecha, BigDecimal ingresos, BigDecimal egresos, BigDecimal saldoAcumulado) {
        BigDecimal saldoDia = ingresos.subtract(egresos);
        return new CashFlowDayRow(fecha, ingresos, egresos, saldoDia, saldoAcumulado, true);
    }
}
