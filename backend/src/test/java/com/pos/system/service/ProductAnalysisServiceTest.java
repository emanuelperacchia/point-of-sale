package com.pos.system.service;

import com.pos.system.dto.response.ProductAnalysisResponse;
import com.pos.system.repository.ProductRepository;
import com.pos.system.repository.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductAnalysisServiceTest {

    @Mock private SaleRepository saleRepository;
    @Mock private ProductRepository productRepository;

    private ProductAnalysisService productAnalysisService;

    private final LocalDate hasta = LocalDate.of(2026, 6, 18);

    @BeforeEach
    void setUp() {
        productAnalysisService = new ProductAnalysisService(saleRepository, productRepository);
    }

    @Test
    void analyzeProducts_ShouldReturnABCClassification() {
        // Given — 5 products with Pareto distribution (approx 80/20)
        java.util.List<Object[]> productSales = new java.util.ArrayList<>();
        productSales.add(new Object[]{1L, "Producto A", "SKU001", "Bebidas", BigDecimal.valueOf(50000), 100L});
        productSales.add(new Object[]{2L, "Producto B", "SKU002", "Bebidas", BigDecimal.valueOf(20000), 50L});
        productSales.add(new Object[]{3L, "Producto C", "SKU003", "Limpieza", BigDecimal.valueOf(15000), 30L});
        productSales.add(new Object[]{4L, "Producto D", "SKU004", "Limpieza", BigDecimal.valueOf(10000), 20L});
        productSales.add(new Object[]{5L, "Producto E", "SKU005", "Otros", BigDecimal.valueOf(5000), 10L});

        when(saleRepository.findAllProductSales(any(), any())).thenReturn(productSales);
        when(saleRepository.findProductsWithoutSalesSince(any())).thenReturn(List.of());
        when(productRepository.countByActiveTrue()).thenReturn(10L);

        // When
        ProductAnalysisResponse res = productAnalysisService.analyzeProducts(hasta, 3, 90);

        // Then
        assertNotNull(res);
        assertEquals("OK", res.getStatus());
        assertNotNull(res.getClasificacionABC());
        assertEquals(5, res.getClasificacionABC().size());
        assertNotNull(res.getResumen());

        // Producto A should be class A (50k/100k = 50% cumulative, < 80%)
        assertEquals("A", res.getClasificacionABC().get(0).getClasificacion());
        // Producto A + B = 70k/100k = 70% cumulative → A & A
        assertEquals("A", res.getClasificacionABC().get(1).getClasificacion());
        // Producto A + B + C = 85k/100k = 85% cumulative → B (>80%, <=95%)
        assertEquals("B", res.getClasificacionABC().get(2).getClasificacion());
        // D = 95% cumulative → B
        assertEquals("B", res.getClasificacionABC().get(3).getClasificacion());
        // E = 100% cumulative → C
        assertEquals("C", res.getClasificacionABC().get(4).getClasificacion());

        // Summary
        assertEquals(5, res.getResumen().getTotalProductos());
        assertEquals(2, res.getResumen().getProductosClaseA());
        assertEquals(2, res.getResumen().getProductosClaseB());
        assertEquals(1, res.getResumen().getProductosClaseC());
    }

    @Test
    void analyzeProducts_WhenNoSales_ShouldReturnEmpty() {
        when(saleRepository.findAllProductSales(any(), any())).thenReturn(List.of());
        when(saleRepository.findProductsWithoutSalesSince(any())).thenReturn(List.of());
        when(productRepository.countByActiveTrue()).thenReturn(10L);

        ProductAnalysisResponse res = productAnalysisService.analyzeProducts(hasta, 3, 90);

        assertNotNull(res);
        assertEquals("OK", res.getStatus());
        assertTrue(res.getClasificacionABC().isEmpty());
    }

    @Test
    void analyzeProducts_WhenRepoFails_ShouldReturnError() {
        when(saleRepository.findAllProductSales(any(), any()))
                .thenThrow(new RuntimeException("DB Error"));

        ProductAnalysisResponse res = productAnalysisService.analyzeProducts(hasta, 3, 90);

        assertEquals("ERROR", res.getStatus());
    }
}
