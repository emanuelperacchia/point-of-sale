package com.pos.system.service.promotion;

import com.pos.system.entity.Promotion;
import com.pos.system.entity.PromotionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class DosX1Strategy implements PromotionStrategy {

    @Override
    public PromotionType getType() {
        return PromotionType.DOSX1;
    }

    @Override
    public BigDecimal calculateDiscount(Promotion promotion, BigDecimal unitPrice, int quantity) {
        // Each group of 2: one free. Discount = (quantity / 3) * unitPrice
        // Groups of 3 items (pay 2, get 1 free)
        int groups = quantity / 3;
        int remainder = quantity % 3;
        BigDecimal totalDiscount = unitPrice.multiply(BigDecimal.valueOf(groups));
        return totalDiscount.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean replacesPrice() {
        return true;
    }
}
