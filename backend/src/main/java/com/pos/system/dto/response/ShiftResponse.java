package com.pos.system.dto.response;

import com.pos.system.entity.ShiftStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Respuesta pública con los datos de un turno de caja.
 */
@Builder
public record ShiftResponse(
        Long id,
        Long cajeroId,
        String cajeroNombre,
        Long sucursalId,
        ShiftStatus estado,
        BigDecimal montoApertura,
        BigDecimal montoCierre,
        BigDecimal diferencia,
        LocalDateTime fechaApertura,
        LocalDateTime fechaCierre
) {}
