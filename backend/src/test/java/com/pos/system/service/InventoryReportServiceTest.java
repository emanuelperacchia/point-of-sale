package com.pos.system.service;

import com.pos.system.repository.ProductRepository;
import com.pos.system.repository.ProductStockRepository;
import com.pos.system.repository.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryReportServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductStockRepository productStockRepository;
    @Mock private StockMovementRepository stockMovementRepository;

    private InventoryReportService inventoryReportService;

    @BeforeEach
    void setUp() {
        inventoryReportService = new InventoryReportService(
                productRepository, productStockRepository, stockMovementRepository);
    }

    @Test
    void getInventoryReport_ShouldReturnReport() {
        when(productStockRepository.getTotalInventoryValueGlobal()).thenReturn(BigDecimal.valueOf(50000));
        when(productRepository.countByActiveTrue()).thenReturn(100L);
        when(productStockRepository.countProductsBelowMinimum()).thenReturn(5L);
        when(stockMovementRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(java.util.List.of());

        var res = inventoryReportService.getInventoryReport();

        assertNotNull(res);
        assertEquals("OK", res.getStatus());
        assertNotNull(res.getValorizacion());
        assertEquals(0, BigDecimal.valueOf(50000).compareTo(res.getValorizacion().getValorTotal()));
        assertEquals(100, res.getValorizacion().getTotalProducts());
        assertEquals(5, res.getValorizacion().getBajoStock());
    }

    @Test
    void getInventoryReport_WhenRepoFails_ShouldReturnError() {
        when(productStockRepository.getTotalInventoryValueGlobal())
                .thenThrow(new RuntimeException("DB Error"));

        var res = inventoryReportService.getInventoryReport();
        assertEquals("ERROR", res.getStatus());
    }
}
