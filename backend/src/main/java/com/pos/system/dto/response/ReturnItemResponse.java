package com.pos.system.dto.response;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Item de una devolución.
 */
@Builder
public record ReturnItemResponse(
        Long id,
        Long saleItemId,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {}
