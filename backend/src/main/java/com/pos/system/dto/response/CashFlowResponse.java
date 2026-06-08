package com.pos.system.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Respuesta del flujo de caja con datos reales, proyección y alertas.
 */
public record CashFlowResponse(
        List<CashFlowDayRow> dias,
        boolean alertaSaldoNegativo,
        LocalDate primerDiaSaldoNegativo
) {
    public static CashFlowResponse sinAlerta(List<CashFlowDayRow> dias) {
        return new CashFlowResponse(dias, false, null);
    }

    public static CashFlowResponse conAlerta(List<CashFlowDayRow> dias, LocalDate primerDiaNegativo) {
        return new CashFlowResponse(dias, true, primerDiaNegativo);
    }
}
