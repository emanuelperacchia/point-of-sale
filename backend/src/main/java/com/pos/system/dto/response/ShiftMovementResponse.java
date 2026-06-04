package com.pos.system.dto.response;

import com.pos.system.entity.ShiftMovementType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Respuesta pública con los datos de un movimiento manual de caja.
 */
@Builder
public record ShiftMovementResponse(
        Long id,
        Long shiftId,
        ShiftMovementType tipo,
        BigDecimal monto,
        String motivo,
        String usuarioNombre,
        LocalDateTime createdAt
) {}
