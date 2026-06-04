package com.pos.system.service.promotion;

import com.pos.system.entity.Promotion;
import com.pos.system.entity.PromotionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class MontoFijoStrategy implements PromotionStrategy {

    @Override
    public PromotionType getType() {
        return PromotionType.MONTO_FIJO;
    }

    @Override
    public BigDecimal calculateDiscount(Promotion promotion, BigDecimal unitPrice, int quantity) {
        // El valor es el descuento fijo por unidad
        BigDecimal discountPerUnit = promotion.getValor();
        BigDecimal totalDiscount = discountPerUnit.multiply(BigDecimal.valueOf(quantity));
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(quantity));
        return totalDiscount.min(total).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean replacesPrice() {
        return false;
    }
}
