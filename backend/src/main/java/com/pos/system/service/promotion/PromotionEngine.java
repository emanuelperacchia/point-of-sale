package com.pos.system.service.promotion;

import com.pos.system.dto.response.CouponResponse;
import com.pos.system.dto.response.DiscountResult;
import com.pos.system.entity.*;
import com.pos.system.repository.CouponRepository;
import com.pos.system.repository.CouponUsageRepository;
import com.pos.system.repository.PromotionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Stateless engine that evaluates promotions and coupons against a set of cart items.
 * Uses Strategy pattern — each PromotionType has a corresponding PromotionStrategy.
 */
@Service
@RequiredArgsConstructor
public class PromotionEngine {

    private final PromotionRepository promotionRepository;
    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final List<PromotionStrategy> strategyList;

    private final Map<PromotionType, PromotionStrategy> strategies = new HashMap<>();

    @PostConstruct
    void initStrategies() {
        for (PromotionStrategy s : strategyList) {
            strategies.put(s.getType(), s);
        }
    }

    /**
     * Evaluate all active promotions against cart items.
     *
     * @param items      items in the cart (productId, quantity, unitPrice)
     * @param couponCode optional coupon code
     * @return DiscountResult with per-item discounts and totals
     */
    public DiscountResult evaluate(
            List<CartItem> items,
            String couponCode
    ) {
        if (items == null || items.isEmpty()) {
            return DiscountResult.builder()
                    .itemsDiscount(List.of())
                    .totalDiscount(BigDecimal.ZERO)
                    .build();
        }

        LocalDate today = LocalDate.now();
        List<DiscountResult.ItemDiscount> itemDiscounts = new ArrayList<>();
        BigDecimal totalItemDiscount = BigDecimal.ZERO;

        // 1. Load active promotions
        List<Promotion> activePromotions = promotionRepository.findActivePromotions(today);

        // Separate by scope
        List<Promotion> productPromos = filterByScope(activePromotions, PromotionScope.PRODUCTO);
        List<Promotion> categoryPromos = filterByScope(activePromotions, PromotionScope.CATEGORIA);
        List<Promotion> cartPromos = filterByScope(activePromotions, PromotionScope.CARRITO);

        // Evaluate each item
        for (int i = 0; i < items.size(); i++) {
            CartItem item = items.get(i);

            BigDecimal bestDiscount = BigDecimal.ZERO;
            String bestPromoName = null;
            Long bestPromoId = null;
            String bestDescription = null;

            // Product-level promotions
            for (Promotion promo : productPromos) {
                if (promo.getProductoIds().contains(item.productId())) {
                    DiscountResult.ItemDiscount result = applyPromotion(promo, item, (long) i);
                    if (result != null && result.discountAmount().compareTo(bestDiscount) > 0) {
                        bestDiscount = result.discountAmount();
                        bestPromoName = result.promotionName();
                        bestPromoId = result.promotionId();
                        bestDescription = result.description();
                    }
                }
            }

            // Category-level promotions
            for (Promotion promo : categoryPromos) {
                if (item.categoryId() != null && promo.getCategoriaIds().contains(item.categoryId())) {
                    DiscountResult.ItemDiscount result = applyPromotion(promo, item, (long) i);
                    if (result != null && result.discountAmount().compareTo(bestDiscount) > 0) {
                        bestDiscount = result.discountAmount();
                        bestPromoName = result.promotionName();
                        bestPromoId = result.promotionId();
                        bestDescription = result.description();
                    }
                }
            }

            if (bestDiscount.compareTo(BigDecimal.ZERO) > 0) {
                itemDiscounts.add(DiscountResult.ItemDiscount.builder()
                        .saleItemIndex((long) i)
                        .productId(item.productId())
                        .productName(item.productName())
                        .promotionName(bestPromoName)
                        .promotionId(bestPromoId)
                        .discountAmount(bestDiscount)
                        .description(bestDescription)
                        .build());
                totalItemDiscount = totalItemDiscount.add(bestDiscount);
            }
        }

        // 2. Evaluate cart-level promotions (applied on top)
        BigDecimal cartLevelDiscount = BigDecimal.ZERO;
        String cartPromoName = null;
        Long cartPromoId = null;

        for (Promotion promo : cartPromos) {
            for (CartItem item : items) {
                DiscountResult.ItemDiscount result = applyPromotion(promo, item, -1L);
                if (result != null) {
                    cartLevelDiscount = cartLevelDiscount.add(result.discountAmount());
                    cartPromoName = result.promotionName();
                    cartPromoId = result.promotionId();
                }
            }
        }

        // 3. Evaluate coupon
        BigDecimal couponDiscount = BigDecimal.ZERO;
        String appliedCouponCode = null;
        if (couponCode != null && !couponCode.isBlank()) {
            CouponResponse couponResult = validateCoupon(couponCode);
            if (couponResult.valido()) {
                appliedCouponCode = couponCode;
                couponDiscount = couponResult.valor();
            }
        }

        BigDecimal totalDiscount = totalItemDiscount.add(cartLevelDiscount).add(couponDiscount);

        // Add cart-level discount as a single item discount entry
        if (cartLevelDiscount.compareTo(BigDecimal.ZERO) > 0 && !items.isEmpty()) {
            itemDiscounts.add(DiscountResult.ItemDiscount.builder()
                    .saleItemIndex(-1L) // cart-level marker
                    .productId(null)
                    .productName("Descuento por carrito")
                    .promotionName(cartPromoName)
                    .promotionId(cartPromoId)
                    .discountAmount(cartLevelDiscount)
                    .description("Descuento por " + cartPromoName)
                    .build());
        }

        return DiscountResult.builder()
                .itemsDiscount(itemDiscounts)
                .totalDiscount(totalDiscount)
                .appliedCouponCode(appliedCouponCode)
                .couponDiscount(couponDiscount)
                .build();
    }

    /**
     * Validate a coupon code and return its discount value if valid.
     */
    public com.pos.system.dto.response.CouponResponse validateCoupon(String codigo) {
        return couponRepository.findByCodigo(codigo)
                .map(coupon -> {
                    if (!coupon.getActivo()) {
                        return new com.pos.system.dto.response.CouponResponse(coupon.getId(), codigo, coupon.getTipo(), coupon.getValor(), false, "Cupón inactivo");
                    }
                    if (coupon.getFechaVencimiento().isBefore(LocalDate.now())) {
                        return new com.pos.system.dto.response.CouponResponse(coupon.getId(), codigo, coupon.getTipo(), coupon.getValor(), false, "Cupón vencido");
                    }
                    if (coupon.getUsosActuales() >= coupon.getLimiteUsos()) {
                        return new com.pos.system.dto.response.CouponResponse(coupon.getId(), codigo, coupon.getTipo(), coupon.getValor(), false, "Cupón agotado");
                    }
                    return new com.pos.system.dto.response.CouponResponse(coupon.getId(), codigo, coupon.getTipo(), coupon.getValor(), true, "Cupón válido");
                })
                .orElse(new com.pos.system.dto.response.CouponResponse(null, codigo, null, BigDecimal.ZERO, false, "Cupón no encontrado"));
    }

    /**
     * Register coupon usage for a sale.
     */
    public void registerCouponUsage(Long couponId, Long saleId, Long clientId) {
        CouponUsage usage = CouponUsage.builder()
                .couponId(couponId)
                .saleId(saleId)
                .clientId(clientId)
                .build();
        couponUsageRepository.save(usage);

        // Increment usosActuales
        couponRepository.findById(couponId).ifPresent(coupon -> {
            coupon.setUsosActuales(coupon.getUsosActuales() + 1);
            couponRepository.save(coupon);
        });
    }

    // ── Private helpers ──────────────────────────────────────

    private DiscountResult.ItemDiscount applyPromotion(Promotion promotion, CartItem item, Long index) {
        PromotionStrategy strategy = strategies.get(promotion.getTipo());
        if (strategy == null) return null;

        BigDecimal discount = strategy.calculateDiscount(promotion, item.unitPrice(), item.quantity());
        if (discount.compareTo(BigDecimal.ZERO) <= 0) return null;

        String description = switch (promotion.getTipo()) {
            case PORCENTAJE -> promotion.getValor() + "% OFF";
            case MONTO_FIJO -> "$" + promotion.getValor() + " descuento";
            case DOSX1 -> "2x1";
            case TRESX2 -> "3x2";
            case COMPRA_X_LLEVA_Y -> "Compra " + promotion.getCompraX() + " lleva " + promotion.getLlevaY();
        };

        return DiscountResult.ItemDiscount.builder()
                .saleItemIndex(index)
                .productId(item.productId())
                .productName(item.productName())
                .promotionName(promotion.getNombre())
                .promotionId(promotion.getId())
                .discountAmount(discount)
                .description(description)
                .build();
    }

    private List<Promotion> filterByScope(List<Promotion> promotions, PromotionScope scope) {
        return promotions.stream()
                .filter(p -> p.getAlcance() == scope)
                .collect(Collectors.toList());
    }

    // ── Record for cart items ────────────────────────────────

    public record CartItem(
            Long productId,
            String productName,
            Integer quantity,
            BigDecimal unitPrice,
            Long categoryId
    ) {}
}
