package com.pos.system.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado completo de la evaluación de promociones y cupones sobre un carrito.
 */
@Builder
public record DiscountResult(
        List<ItemDiscount> itemsDiscount,
        BigDecimal totalDiscount,
        String appliedCouponCode,
        BigDecimal couponDiscount
) {
    @Builder
    public record ItemDiscount(
            Long saleItemIndex,
            Long productId,
            String productName,
            String promotionName,
            Long promotionId,
            BigDecimal discountAmount,
            String description
    ) {}
}
