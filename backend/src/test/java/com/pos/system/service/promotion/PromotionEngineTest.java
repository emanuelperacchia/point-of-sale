package com.pos.system.service.promotion;

import com.pos.system.entity.Promotion;
import com.pos.system.entity.PromotionScope;
import com.pos.system.entity.PromotionType;
import com.pos.system.repository.CouponRepository;
import com.pos.system.repository.CouponUsageRepository;
import com.pos.system.repository.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionEngineTest {

    @Mock private PromotionRepository promotionRepository;
    @Mock private CouponRepository couponRepository;
    @Mock private CouponUsageRepository couponUsageRepository;

    private PromotionEngine engine;
    private List<PromotionStrategy> strategies;

    @BeforeEach
    void setUp() {
        strategies = List.of(
                new PorcentajeStrategy(),
                new MontoFijoStrategy(),
                new DosX1Strategy(),
                new TresX2Strategy(),
                new CompraXLlevaYStrategy()
        );
        engine = new PromotionEngine(promotionRepository, couponRepository,
                couponUsageRepository, strategies);
        engine.initStrategies();
    }

    @Test
    void evaluate_WithNoPromotions_ShouldReturnZeroDiscount() {
        when(promotionRepository.findActivePromotions(any(LocalDate.class)))
                .thenReturn(List.of());

        var result = engine.evaluate(List.of(
                new PromotionEngine.CartItem(1L, "Producto A", 2, BigDecimal.valueOf(1000), null)
        ), null);

        assertEquals(BigDecimal.ZERO, result.totalDiscount());
        assertTrue(result.itemsDiscount().isEmpty());
    }

    @Test
    void evaluate_WithPorcentajePromotion_ShouldApplyPercentDiscount() {
        Promotion promo = Promotion.builder()
                .id(1L).nombre("10% OFF").tipo(PromotionType.PORCENTAJE)
                .valor(BigDecimal.valueOf(10)).prioridad(10)
                .alcance(PromotionScope.PRODUCTO).activa(true)
                .fechaDesde(LocalDate.now().minusDays(1))
                .fechaHasta(LocalDate.now().plusDays(1))
                .productoIds(List.of(1L))
                .build();

        when(promotionRepository.findActivePromotions(any())).thenReturn(List.of(promo));

        var result = engine.evaluate(List.of(
                new PromotionEngine.CartItem(1L, "Producto A", 2, BigDecimal.valueOf(1000), null)
        ), null);

        // 2 * 1000 = 2000, 10% = 200
        assertEquals(0, BigDecimal.valueOf(200).compareTo(result.totalDiscount()));
        assertEquals(1, result.itemsDiscount().size());
        assertEquals("10% OFF", result.itemsDiscount().get(0).promotionName());
    }

    @Test
    void evaluate_WithMontoFijo_ShouldApplyFixedDiscount() {
        Promotion promo = Promotion.builder()
                .id(2L).nombre("Descuento fijo").tipo(PromotionType.MONTO_FIJO)
                .valor(BigDecimal.valueOf(100)).prioridad(10)
                .alcance(PromotionScope.PRODUCTO).activa(true)
                .fechaDesde(LocalDate.now().minusDays(1))
                .fechaHasta(LocalDate.now().plusDays(1))
                .productoIds(List.of(1L))
                .build();

        when(promotionRepository.findActivePromotions(any())).thenReturn(List.of(promo));

        var result = engine.evaluate(List.of(
                new PromotionEngine.CartItem(1L, "Producto A", 2, BigDecimal.valueOf(1000), null)
        ), null);

        // 100 * 2 = 200
        assertEquals(0, BigDecimal.valueOf(200).compareTo(result.totalDiscount()));
    }

    @Test
    void evaluate_WithDosX1_ShouldCalculateCorrectDiscount() {
        Promotion promo = Promotion.builder()
                .id(3L).nombre("2x1").tipo(PromotionType.DOSX1)
                .valor(BigDecimal.ZERO).prioridad(10)
                .alcance(PromotionScope.PRODUCTO).activa(true)
                .fechaDesde(LocalDate.now().minusDays(1))
                .fechaHasta(LocalDate.now().plusDays(1))
                .productoIds(List.of(1L))
                .build();

        when(promotionRepository.findActivePromotions(any())).thenReturn(List.of(promo));

        var result = engine.evaluate(List.of(
                new PromotionEngine.CartItem(1L, "Producto A", 3, BigDecimal.valueOf(1000), null)
        ), null);

        // 3 items: 1 group of 3 (pay 2 get 1 free), discount = 1 * 1000 = 1000
        assertEquals(0, BigDecimal.valueOf(1000).compareTo(result.totalDiscount()));
    }

    @Test
    void evaluate_WithHigherPriority_ShouldWin() {
        Promotion low = Promotion.builder()
                .id(4L).nombre("5% OFF").tipo(PromotionType.PORCENTAJE)
                .valor(BigDecimal.valueOf(5)).prioridad(1)
                .alcance(PromotionScope.PRODUCTO).activa(true)
                .fechaDesde(LocalDate.now().minusDays(1))
                .fechaHasta(LocalDate.now().plusDays(1))
                .productoIds(List.of(1L))
                .build();

        Promotion high = Promotion.builder()
                .id(5L).nombre("15% OFF").tipo(PromotionType.PORCENTAJE)
                .valor(BigDecimal.valueOf(15)).prioridad(10)
                .alcance(PromotionScope.PRODUCTO).activa(true)
                .fechaDesde(LocalDate.now().minusDays(1))
                .fechaHasta(LocalDate.now().plusDays(1))
                .productoIds(List.of(1L))
                .build();

        when(promotionRepository.findActivePromotions(any())).thenReturn(List.of(low, high));

        var result = engine.evaluate(List.of(
                new PromotionEngine.CartItem(1L, "Producto A", 1, BigDecimal.valueOf(1000), null)
        ), null);

        // Higher priority (10) wins: 15% = 150
        assertEquals(0, BigDecimal.valueOf(150).compareTo(result.totalDiscount()));
        assertEquals("15% OFF", result.itemsDiscount().get(0).promotionName());
    }

    @Test
    void evaluate_WithTresX2_ShouldCalculateCorrectDiscount() {
        Promotion promo = Promotion.builder()
                .id(6L).nombre("3x2").tipo(PromotionType.TRESX2)
                .valor(BigDecimal.ZERO).prioridad(10)
                .alcance(PromotionScope.PRODUCTO).activa(true)
                .fechaDesde(LocalDate.now().minusDays(1))
                .fechaHasta(LocalDate.now().plusDays(1))
                .productoIds(List.of(1L))
                .build();

        when(promotionRepository.findActivePromotions(any())).thenReturn(List.of(promo));

        var result = engine.evaluate(List.of(
                new PromotionEngine.CartItem(1L, "Producto A", 5, BigDecimal.valueOf(1000), null)
        ), null);

        // 5 items: 1 group of 5 (pay 3 get 2 free), discount = 2 * 1000 = 2000
        assertEquals(0, BigDecimal.valueOf(2000).compareTo(result.totalDiscount()));
    }

    @Test
    void evaluate_WithCompraXLlevaY_ShouldCalculateCorrectDiscount() {
        Promotion promo = Promotion.builder()
                .id(7L).nombre("Compra 2 lleva 1").tipo(PromotionType.COMPRA_X_LLEVA_Y)
                .valor(BigDecimal.ZERO).prioridad(10)
                .alcance(PromotionScope.PRODUCTO).activa(true)
                .fechaDesde(LocalDate.now().minusDays(1))
                .fechaHasta(LocalDate.now().plusDays(1))
                .compraX(2).llevaY(1)
                .productoIds(List.of(1L))
                .build();

        when(promotionRepository.findActivePromotions(any())).thenReturn(List.of(promo));

        var result = engine.evaluate(List.of(
                new PromotionEngine.CartItem(1L, "Producto A", 6, BigDecimal.valueOf(500), null)
        ), null);

        // 6 items: 2 groups of 3 (buy 2 get 1 free), discount = 2 * 500 = 1000
        assertEquals(0, BigDecimal.valueOf(1000).compareTo(result.totalDiscount()));
    }

    @Test
    void evaluate_WithExpiredPromotion_ShouldNotApply() {
        Promotion expired = Promotion.builder()
                .id(8L).nombre("Expirada").tipo(PromotionType.PORCENTAJE)
                .valor(BigDecimal.valueOf(50)).prioridad(99)
                .alcance(PromotionScope.PRODUCTO).activa(true)
                .fechaDesde(LocalDate.now().minusDays(10))
                .fechaHasta(LocalDate.now().minusDays(1)) // expired yesterday
                .productoIds(List.of(1L))
                .build();

        when(promotionRepository.findActivePromotions(any())).thenReturn(List.of());

        var result = engine.evaluate(List.of(
                new PromotionEngine.CartItem(1L, "Prod", 1, BigDecimal.valueOf(1000), null)
        ), null);

        assertEquals(BigDecimal.ZERO, result.totalDiscount());
    }

    @Test
    void evaluate_WithCategoryScope_ShouldApplyToMatchingCategory() {
        Promotion promo = Promotion.builder()
                .id(9L).nombre("Cat desc").tipo(PromotionType.PORCENTAJE)
                .valor(BigDecimal.valueOf(10)).prioridad(10)
                .alcance(PromotionScope.CATEGORIA).activa(true)
                .fechaDesde(LocalDate.now().minusDays(1))
                .fechaHasta(LocalDate.now().plusDays(1))
                .categoriaIds(List.of(5L))
                .build();

        when(promotionRepository.findActivePromotions(any())).thenReturn(List.of(promo));

        var result = engine.evaluate(List.of(
                new PromotionEngine.CartItem(1L, "Prod", 2, BigDecimal.valueOf(1000), 5L)
        ), null);

        // 2 * 1000 = 2000, 10% = 200
        assertEquals(0, BigDecimal.valueOf(200).compareTo(result.totalDiscount()));
    }

    @Test
    void evaluate_WithCartScope_ShouldApplyToAll() {
        Promotion promo = Promotion.builder()
                .id(10L).nombre("Carrito desc").tipo(PromotionType.MONTO_FIJO)
                .valor(BigDecimal.valueOf(500)).prioridad(10)
                .alcance(PromotionScope.CARRITO).activa(true)
                .fechaDesde(LocalDate.now().minusDays(1))
                .fechaHasta(LocalDate.now().plusDays(1))
                .build();

        when(promotionRepository.findActivePromotions(any())).thenReturn(List.of(promo));

        var result = engine.evaluate(List.of(
                new PromotionEngine.CartItem(1L, "Prod A", 1, BigDecimal.valueOf(2000), null),
                new PromotionEngine.CartItem(2L, "Prod B", 1, BigDecimal.valueOf(3000), null)
        ), null);

        // Cart-level monto fijo applies per item: 500 * 2 items = 1000
        assertEquals(0, BigDecimal.valueOf(1000).compareTo(result.totalDiscount()));
        assertTrue(result.itemsDiscount().stream().anyMatch(d -> d.saleItemIndex() == -1));
    }

    @Test
    void validateCoupon_WithValidCoupon_ShouldReturnValid() {
        var coupon = new com.pos.system.entity.Coupon();
        coupon.setId(1L);
        coupon.setCodigo("VERANO10");
        coupon.setTipo(PromotionType.PORCENTAJE);
        coupon.setValor(BigDecimal.valueOf(10));
        coupon.setFechaVencimiento(LocalDate.now().plusDays(30));
        coupon.setLimiteUsos(100);
        coupon.setUsosActuales(5);
        coupon.setActivo(true);

        when(couponRepository.findByCodigo("VERANO10")).thenReturn(Optional.of(coupon));

        var result = engine.validateCoupon("VERANO10");

        assertTrue(result.valido());
        assertEquals("VERANO10", result.codigo());
    }

    @Test
    void validateCoupon_WithExpiredCoupon_ShouldReturnInvalid() {
        var coupon = new com.pos.system.entity.Coupon();
        coupon.setId(2L);
        coupon.setCodigo("EXPIRADO");
        coupon.setTipo(PromotionType.MONTO_FIJO);
        coupon.setValor(BigDecimal.valueOf(1000));
        coupon.setFechaVencimiento(LocalDate.now().minusDays(1));
        coupon.setLimiteUsos(10);
        coupon.setUsosActuales(0);
        coupon.setActivo(true);

        when(couponRepository.findByCodigo("EXPIRADO")).thenReturn(Optional.of(coupon));

        var result = engine.validateCoupon("EXPIRADO");

        assertFalse(result.valido());
        assertTrue(result.mensaje().contains("vencido"));
    }

    @Test
    void validateCoupon_WithExhaustedCoupon_ShouldReturnInvalid() {
        var coupon = new com.pos.system.entity.Coupon();
        coupon.setId(3L);
        coupon.setCodigo("AGOTADO");
        coupon.setTipo(PromotionType.PORCENTAJE);
        coupon.setValor(BigDecimal.valueOf(20));
        coupon.setFechaVencimiento(LocalDate.now().plusDays(30));
        coupon.setLimiteUsos(10);
        coupon.setUsosActuales(10);
        coupon.setActivo(true);

        when(couponRepository.findByCodigo("AGOTADO")).thenReturn(Optional.of(coupon));

        var result = engine.validateCoupon("AGOTADO");

        assertFalse(result.valido());
        assertTrue(result.mensaje().contains("agotado"));
    }

    @Test
    void validateCoupon_WithInactiveCoupon_ShouldReturnInvalid() {
        var coupon = new com.pos.system.entity.Coupon();
        coupon.setId(4L);
        coupon.setCodigo("INACTIVO");
        coupon.setActivo(false);

        when(couponRepository.findByCodigo("INACTIVO")).thenReturn(Optional.of(coupon));

        var result = engine.validateCoupon("INACTIVO");

        assertFalse(result.valido());
        assertTrue(result.mensaje().contains("inactivo"));
    }

    @Test
    void validateCoupon_WithUnknownCode_ShouldReturnNotFound() {
        when(couponRepository.findByCodigo("NOEXISTE")).thenReturn(Optional.empty());

        var result = engine.validateCoupon("NOEXISTE");

        assertFalse(result.valido());
        assertTrue(result.mensaje().contains("no encontrado"));
    }

    @Test
    void registerCouponUsage_ShouldIncrementUsos() {
        var coupon = new com.pos.system.entity.Coupon();
        coupon.setId(1L);
        coupon.setUsosActuales(5);

        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

        engine.registerCouponUsage(1L, 100L, 1L);

        verify(couponUsageRepository).save(any());
        assertEquals(6, coupon.getUsosActuales());
        verify(couponRepository).save(coupon);
    }
}
