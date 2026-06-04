package com.pos.system.service;

import com.pos.system.entity.Product;
import com.pos.system.entity.ProductStock;
import com.pos.system.entity.Warehouse;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KardexServiceTest {

    @Mock private StockMovementRepository stockMovementRepository;
    @Mock private ProductStockRepository productStockRepository;
    @Mock private ProductRepository productRepository;
    @Mock private WarehouseRepository warehouseRepository;

    private KardexService kardexService;

    @BeforeEach
    void setUp() {
        kardexService = new KardexService(stockMovementRepository, productStockRepository,
                productRepository, warehouseRepository);
    }

    @Test
    void getTotalStock_WhenProductExists_ShouldReturnTotal() {
        when(productStockRepository.getTotalStockByProduct(1L)).thenReturn(BigDecimal.TEN);
        BigDecimal total = kardexService.getTotalStock(1L);
        assertEquals(BigDecimal.TEN, total);
    }

    @Test
    void getTotalStock_WhenNoStock_ShouldReturnZero() {
        when(productStockRepository.getTotalStockByProduct(1L)).thenReturn(null);
        BigDecimal total = kardexService.getTotalStock(1L);
        assertEquals(BigDecimal.ZERO, total);
    }

    @Test
    void getProductStockStatus_WhenProductNotFound_ShouldThrow() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> kardexService.getProductStockStatus(99L));
    }

    @Test
    void getProductKardex_ShouldReturnPagedMovements() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setSku("TST-001");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(stockMovementRepository.findByProductOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Page.empty());

        Page<?> result = kardexService.getProductKardex(1L, PageRequest.of(0, 20));
        assertNotNull(result);
    }

    @Test
    void getProductStockByWarehouse_ShouldReturnStockStatus() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Test");
        product.setSku("TST-001");

        Warehouse warehouse = Warehouse.builder().id(1L).name("Main").build();

        ProductStock stock = ProductStock.builder()
                .product(product)
                .warehouse(warehouse)
                .currentStock(BigDecimal.valueOf(50))
                .minimumStock(BigDecimal.valueOf(10))
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(productStockRepository.findByProductAndWarehouse(product, warehouse))
                .thenReturn(Optional.of(stock));

        var result = kardexService.getProductStockByWarehouse(1L, 1L);
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(50), result.getCurrentStock());
        assertEquals(BigDecimal.valueOf(50), result.getAvailableStock());
    }
}