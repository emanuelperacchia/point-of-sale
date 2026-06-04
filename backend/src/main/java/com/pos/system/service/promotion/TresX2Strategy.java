package com.pos.system.service.promotion;

import com.pos.system.entity.Promotion;
import com.pos.system.entity.PromotionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class TresX2Strategy implements PromotionStrategy {

    @Override
    public PromotionType getType() {
        return PromotionType.TRESX2;
    }

    @Override
    public BigDecimal calculateDiscount(Promotion promotion, BigDecimal unitPrice, int quantity) {
        // Each group of 5 (pay 3, get 2 free): one free = 2 * unitPrice per group
        int groups = quantity / 5;
        int remainder = quantity % 5;
        BigDecimal totalDiscount = unitPrice.multiply(BigDecimal.valueOf(groups * 2L));
        return totalDiscount.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean replacesPrice() {
        return true;
    }
}
