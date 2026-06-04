package com.pos.system.dto.response;

import com.pos.system.entity.ShiftStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Reporte de cierre de turno con detalle de ventas, movimientos y diferencia.
 */
@Builder
public record ShiftReportResponse(
        Long shiftId,
        ShiftStatus estado,
        BigDecimal montoApertura,
        BigDecimal totalVentasEfectivo,
        Map<String, BigDecimal> ventasPorMetodoPago,
        BigDecimal totalIngresos,
        BigDecimal totalRetiros,
        List<ShiftMovementResponse> movimientos,
        BigDecimal montoEsperado,
        BigDecimal montoCierreDeclarado,
        BigDecimal diferencia,
        LocalDateTime fechaApertura,
        LocalDateTime fechaCierre
) {}
