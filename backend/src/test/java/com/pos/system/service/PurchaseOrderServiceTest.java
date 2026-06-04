package com.pos.system.service;

import com.pos.system.dto.request.PurchaseOrderRequest;
import com.pos.system.dto.response.PurchaseOrderResponse;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private PurchaseOrderDetailRepository detailRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private ProductRepository productRepository;

    private PurchaseOrderService purchaseOrderService;

    private Supplier supplier;
    private Warehouse warehouse;
    private Product product;
    private User user;
    private PurchaseOrder order;
    private PurchaseOrderRequest request;

    @BeforeEach
    void setUp() {
        purchaseOrderService = new PurchaseOrderService(
                purchaseOrderRepository, detailRepository, supplierRepository,
                warehouseRepository, productRepository);

        supplier = Supplier.builder()
                .id(1L).code("PROV001").taxId("30-12345678-9")
                .businessName("Proveedor S.A.")
                .paymentTerm(Supplier.PaymentTerm.NET_30)
                .active(true)
                .build();

        warehouse = Warehouse.builder()
                .id(1L).code("MAIN").name("Bodega Principal")
                .active(true)
                .build();

        product = Product.builder()
                .id(1L).name("Producto Test").sku("TST-001")
                .price(BigDecimal.valueOf(100))
                .stock(10)
                .active(true)
                .build();

        user = User.builder()
                .id(1L).firstName("Admin").lastName("User")
                .email("admin@test.com")
                .build();

        order = PurchaseOrder.builder()
                .id(1L).orderNumber("PO-00001")
                .supplier(supplier).warehouse(warehouse)
                .orderDate(LocalDate.now())
                .status(PurchaseOrder.OrderStatus.BORRADOR)
                .paymentTerm(Supplier.PaymentTerm.NET_30)
                .subtotal(BigDecimal.valueOf(1000))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.valueOf(210))
                .total(BigDecimal.valueOf(1210))
                .createdBy(user)
                .details(List.of())
                .build();

        request = PurchaseOrderRequest.builder()
                .supplierId(1L).warehouseId(1L)
                .orderDate(LocalDate.now())
                .paymentTerm(Supplier.PaymentTerm.NET_30)
                .details(List.of(
                        PurchaseOrderRequest.PurchaseOrderDetailRequest.builder()
                                .productId(1L)
                                .quantity(BigDecimal.valueOf(10))
                                .unitPrice(BigDecimal.valueOf(100))
                                .build()
                ))
                .build();
    }

    @Test
    void findAll_ShouldReturnList() {
        when(purchaseOrderRepository.findAll()).thenReturn(List.of(order));

        List<PurchaseOrderResponse> result = purchaseOrderService.findAll();

        assertEquals(1, result.size());
        assertEquals("PO-00001", result.get(0).getOrderNumber());
    }

    @Test
    void findById_WhenExists_ShouldReturnOrder() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        PurchaseOrderResponse result = purchaseOrderService.findById(1L);

        assertEquals("PO-00001", result.getOrderNumber());
        assertEquals("Proveedor S.A.", result.getSupplierName());
    }

    @Test
    void findById_WhenNotExists_ShouldThrow() {
        when(purchaseOrderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> purchaseOrderService.findById(99L));
    }

    @Test
    void findByOrderNumber_ShouldReturnOrder() {
        when(purchaseOrderRepository.findByOrderNumber("PO-00001")).thenReturn(Optional.of(order));

        PurchaseOrderResponse result = purchaseOrderService.findByOrderNumber("PO-00001");

        assertEquals("PO-00001", result.getOrderNumber());
    }

    @Test
    void findPendingOrders_ShouldReturnList() {
        when(purchaseOrderRepository.findPendingOrders()).thenReturn(List.of(order));

        List<PurchaseOrderResponse> result = purchaseOrderService.findPendingOrders();

        assertEquals(1, result.size());
    }

    @Test
    void create_WithInactiveSupplier_ShouldThrow() {
        supplier.setActive(false);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        assertThrows(BadRequestException.class,
                () -> purchaseOrderService.create(request, user));
    }

    @Test
    void create_WithValidData_ShouldSucceed() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(purchaseOrderRepository.getNextOrderNumber("PO-%")).thenReturn(0);
        when(purchaseOrderRepository.save(any())).thenReturn(order);

        PurchaseOrderResponse result = purchaseOrderService.create(request, user);

        assertEquals("PO-00001", result.getOrderNumber());
        verify(purchaseOrderRepository).save(any());
    }

    @Test
    void approve_WithBorradorStatus_ShouldChangeToConfirmada() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.save(any())).thenReturn(order);

        PurchaseOrderResponse result = purchaseOrderService.approve(1L, user);

        assertNotNull(result);
        verify(purchaseOrderRepository).save(order);
    }

    @Test
    void cancel_ShouldChangeStatus() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.save(any())).thenReturn(order);

        PurchaseOrderResponse result = purchaseOrderService.cancel(1L, "Motivo de prueba");

        assertNotNull(result);
        verify(purchaseOrderRepository).save(order);
    }

    @Test
    void delete_WithBorradorStatus_ShouldDelete() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        purchaseOrderService.delete(1L);

        verify(purchaseOrderRepository).delete(order);
    }

    @Test
    void delete_WithConfirmedStatus_ShouldThrow() {
        order.setStatus(PurchaseOrder.OrderStatus.CONFIRMADA);
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class, () -> purchaseOrderService.delete(1L));
    }

    @Test
    void findOverdueOrders_ShouldReturnList() {
        when(purchaseOrderRepository.findOverdueOrders(any())).thenReturn(List.of(order));

        List<PurchaseOrderResponse> result = purchaseOrderService.findOverdueOrders();

        assertEquals(1, result.size());
    }

    @Test
    void update_WithNonBorradorStatus_ShouldThrow() {
        order.setStatus(PurchaseOrder.OrderStatus.ENVIADA);
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class,
                () -> purchaseOrderService.update(1L, request));
    }
}
