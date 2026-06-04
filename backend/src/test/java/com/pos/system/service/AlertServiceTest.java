package com.pos.system.service;

import com.pos.system.entity.*;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock private AlertRepository alertRepository;
    @Mock private ProductStockRepository productStockRepository;
    @Mock private UserRepository userRepository;

    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(alertRepository, productStockRepository, userRepository);
    }

    @Test
    void countActiveAlerts_ShouldReturnCount() {
        when(alertRepository.countByResolvedFalse()).thenReturn(5L);
        assertEquals(5L, alertService.countActiveAlerts());
    }

    @Test
    void countUnreadAlerts_ShouldReturnCount() {
        when(alertRepository.countByResolvedFalseAndReadFalse()).thenReturn(3L);
        assertEquals(3L, alertService.countUnreadAlerts());
    }

    @Test
    void resolveAlert_ShouldMarkAsResolved() {
        User user = User.builder().id(1L).firstName("Admin").lastName("System").build();
        Product product = Product.builder().id(1L).name("Test").sku("TST").build();
        Warehouse warehouse = Warehouse.builder().id(1L).name("Main").build();

        Alert alert = Alert.builder()
                .id(1L).product(product).warehouse(warehouse)
                .type(Alert.AlertType.STOCK_BAJO)
                .severity(Alert.AlertSeverity.MEDIUM)
                .currentStock(BigDecimal.ZERO).minimumStock(BigDecimal.TEN)
                .build();

        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

        var result = alertService.resolveAlert(1L, 1L);
        assertTrue(result.getResolved());
        assertNotNull(result.getResolvedAt());
    }

    @Test
    void markAsRead_WhenNotFound_ShouldThrow() {
        when(alertRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> alertService.markAsRead(99L));
    }

    @Test
    void generateLowStockAlerts_ShouldCreateAlerts() {
        Product product = Product.builder().id(1L).name("Test").sku("TST").build();
        Warehouse warehouse = Warehouse.builder().id(1L).name("Main").build();
        ProductStock lowStock = ProductStock.builder()
                .product(product).warehouse(warehouse)
                .currentStock(BigDecimal.ONE).minimumStock(BigDecimal.TEN)
                .build();

        when(productStockRepository.findProductsBelowMinimum())
                .thenReturn(List.of(lowStock));
        when(alertRepository.findActiveAlertsByProduct(1L))
                .thenReturn(List.of());
        when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

        int count = alertService.generateLowStockAlerts();
        assertEquals(1, count);
        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void generateLowStockAlerts_WhenAlreadyHasAlert_ShouldSkipDuplicate() {
        Product product = Product.builder().id(1L).name("Test").sku("TST").build();
        Warehouse warehouse = Warehouse.builder().id(1L).name("Main").build();
        ProductStock lowStock = ProductStock.builder()
                .product(product).warehouse(warehouse)
                .currentStock(BigDecimal.ONE).minimumStock(BigDecimal.TEN)
                .build();

        Alert existingAlert = Alert.builder().warehouse(warehouse).build();

        when(productStockRepository.findProductsBelowMinimum())
                .thenReturn(List.of(lowStock));
        when(alertRepository.findActiveAlertsByProduct(1L))
                .thenReturn(List.of(existingAlert));

        int count = alertService.generateLowStockAlerts();
        assertEquals(0, count, "Should not create duplicate alert");
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void getLowStockReport_ShouldReturnReport() {
        Product product = Product.builder().id(1L).name("Test").sku("TST").build();
        Warehouse warehouse = Warehouse.builder().id(1L).name("Main").build();
        ProductStock lowStock = ProductStock.builder()
                .product(product).warehouse(warehouse)
                .currentStock(BigDecimal.ONE).minimumStock(BigDecimal.TEN)
                .maximumStock(BigDecimal.valueOf(100))
                .build();

        when(productStockRepository.findProductsBelowMinimum())
                .thenReturn(List.of(lowStock));

        var report = alertService.getLowStockReport();
        assertEquals(1L, report.getTotalProductsBelowMinimum());
        assertEquals(1, report.getItems().size());
        assertEquals("Test", report.getItems().get(0).getProductName());
    }
}