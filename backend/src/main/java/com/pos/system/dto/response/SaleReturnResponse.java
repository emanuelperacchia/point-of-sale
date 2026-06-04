package com.pos.system.dto.response;

import com.pos.system.entity.PaymentMethod;
import com.pos.system.entity.ReturnStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta pública de una devolución.
 */
@Builder
public record SaleReturnResponse(
        Long id,
        Long saleId,
        ReturnStatus estado,
        String motivo,
        Long aprobadorId,
        BigDecimal montoTotal,
        PaymentMethod metodoDevolucion,
        Long notaCreditoId,
        String referenciaDevolucion,
        List<ReturnItemResponse> items,
        LocalDateTime createdAt
) {}
