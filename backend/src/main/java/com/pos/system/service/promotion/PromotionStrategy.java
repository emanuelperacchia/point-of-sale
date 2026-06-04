package com.pos.system.service.promotion;

import com.pos.system.entity.Promotion;
import com.pos.system.entity.PromotionType;

import java.math.BigDecimal;

/**
 * Strategy interface for calculating discounts per promotion type.
 */
public interface PromotionStrategy {

    PromotionType getType();

    /**
     * Calculate the discount for a single unit given the promotion and unit price.
     */
    BigDecimal calculateDiscount(Promotion promotion, BigDecimal unitPrice, int quantity);

    /**
     * Whether this strategy replaces the item price (2x1, 3x2) or applies as additional discount.
     */
    boolean replacesPrice();
}
