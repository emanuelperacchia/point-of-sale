package com.pos.system.service;

import com.pos.system.dto.request.StockMovementRequest;
import com.pos.system.entity.*;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

    @Mock private ProductStockRepository productStockRepository;
    @Mock private StockMovementRepository stockMovementRepository;
    @Mock private ProductRepository productRepository;
    @Mock private WarehouseRepository warehouseRepository;

    private StockMovementService stockMovementService;

    private Product product;
    private Warehouse warehouse;
    private User user;
    private ProductStock productStock;

    @BeforeEach
    void setUp() {
        stockMovementService = new StockMovementService(
                productStockRepository, stockMovementRepository,
                productRepository, warehouseRepository);

        product = Product.builder().id(1L).name("Test Product").sku("TST-001").price(BigDecimal.valueOf(100)).build();
        warehouse = Warehouse.builder().id(1L).name("Main").code("MAIN").build();
        user = User.builder().id(1L).email("test@test.com").build();
        productStock = ProductStock.builder()
                .product(product).warehouse(warehouse)
                .currentStock(BigDecimal.valueOf(100))
                .minimumStock(BigDecimal.TEN)
                .maximumStock(BigDecimal.valueOf(500))
                .build();
    }

    @Test
    void entryByPurchase_ShouldIncreaseStock() {
        StockMovementRequest request = StockMovementRequest.builder()
                .productId(1L).warehouseId(1L)
                .quantity(BigDecimal.TEN).unitCost(BigDecimal.valueOf(50))
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(productStockRepository.findByProductAndWarehouse(product, warehouse))
                .thenReturn(Optional.of(productStock));
        when(stockMovementRepository.save(any(StockMovement.class)))
                .thenAnswer(i -> i.getArgument(0));

        var result = stockMovementService.entryByPurchase(request, user);
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(110), productStock.getCurrentStock());
    }

    @Test
    void exitBySale_WhenInsufficientStock_ShouldThrow() {
        StockMovementRequest request = StockMovementRequest.builder()
                .productId(1L).warehouseId(1L)
                .quantity(BigDecimal.valueOf(200))
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(productStockRepository.findByProductAndWarehouse(product, warehouse))
                .thenReturn(Optional.of(productStock));

        assertThrows(BadRequestException.class,
                () -> stockMovementService.exitBySale(request, user));
    }

    @Test
    void createMovement_WhenNewProduct_ShouldCreateInitialStock() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(productStockRepository.findByProductAndWarehouse(product, warehouse))
                .thenReturn(Optional.empty());
        when(productStockRepository.save(any(ProductStock.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(stockMovementRepository.save(any(StockMovement.class)))
                .thenAnswer(i -> i.getArgument(0));

        var result = stockMovementService.createMovement(
                1L, 1L, StockMovement.MovementType.ENTRADA_COMPRA,
                BigDecimal.TEN, BigDecimal.valueOf(50), "Test", "DOC-001", user);

        assertNotNull(result);
        verify(productStockRepository, times(2)).save(any(ProductStock.class));
    }

    @Test
    void validateStockAvailable_WhenProductNotFound_ShouldThrow() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> stockMovementService.validateStockAvailable(99L, 1L, BigDecimal.TEN));
    }

    @Test
    void transferStock_ShouldCreateTwoMovements() {
        Warehouse toWarehouse = Warehouse.builder().id(2L).name("Branch").code("BR").build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(toWarehouse));
        when(productStockRepository.findByProductAndWarehouse(product, warehouse))
                .thenReturn(Optional.of(productStock));
        when(productStockRepository.findByProductAndWarehouse(product, toWarehouse))
                .thenReturn(Optional.empty());
        when(productStockRepository.save(any(ProductStock.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(stockMovementRepository.save(any(StockMovement.class)))
                .thenAnswer(i -> i.getArgument(0));

        stockMovementService.transferStock(1L, 1L, 2L, BigDecimal.valueOf(30), "Transfer test", user);

        verify(stockMovementRepository, times(2)).save(any(StockMovement.class));
        assertEquals(BigDecimal.valueOf(70), productStock.getCurrentStock());
    }
}