package com.pos.system.service.promotion;

import com.pos.system.entity.Promotion;
import com.pos.system.entity.PromotionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class CompraXLlevaYStrategy implements PromotionStrategy {

    @Override
    public PromotionType getType() {
        return PromotionType.COMPRA_X_LLEVA_Y;
    }

    @Override
    public BigDecimal calculateDiscount(Promotion promotion, BigDecimal unitPrice, int quantity) {
        int compraX = promotion.getCompraX() != null ? promotion.getCompraX() : 2;
        int llevaY = promotion.getLlevaY() != null ? promotion.getLlevaY() : 1;
        int groupSize = compraX + llevaY;
        if (groupSize == 0) return BigDecimal.ZERO;

        int groups = quantity / groupSize;
        BigDecimal totalDiscount = unitPrice.multiply(BigDecimal.valueOf(groups * (long) llevaY));
        return totalDiscount.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean replacesPrice() {
        return true;
    }
}
