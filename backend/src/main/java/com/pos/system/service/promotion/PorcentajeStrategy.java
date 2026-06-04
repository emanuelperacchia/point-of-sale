package com.pos.system.service.promotion;

import com.pos.system.entity.Promotion;
import com.pos.system.entity.PromotionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PorcentajeStrategy implements PromotionStrategy {

    @Override
    public PromotionType getType() {
        return PromotionType.PORCENTAJE;
    }

    @Override
    public BigDecimal calculateDiscount(Promotion promotion, BigDecimal unitPrice, int quantity) {
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal percent = promotion.getValor().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return total.multiply(percent).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean replacesPrice() {
        return false;
    }
}
