package com.pos.system.service;

import com.pos.system.dto.request.GoodsReceiptRequest;
import com.pos.system.dto.response.GoodsReceiptResponse;
import com.pos.system.dto.request.StockMovementRequest;
import com.pos.system.entity.*;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
class GoodsReceiptServiceTest {

    @Mock private GoodsReceiptRepository goodsReceiptRepository;
    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private PurchaseOrderDetailRepository purchaseOrderDetailRepository;
    @Mock private ProductRepository productRepository;
    @Mock private StockMovementService stockMovementService;

    @Captor private ArgumentCaptor<GoodsReceipt> receiptCaptor;
    @Captor private ArgumentCaptor<PurchaseOrder> orderCaptor;

    private GoodsReceiptService goodsReceiptService;

    private Supplier supplier;
    private Warehouse warehouse;
    private Product product;
    private User user;
    private PurchaseOrder order;
    private PurchaseOrderDetail orderDetail;

    @BeforeEach
    void setUp() {
        goodsReceiptService = new GoodsReceiptService(
                goodsReceiptRepository, purchaseOrderRepository,
                purchaseOrderDetailRepository, productRepository,
                stockMovementService);

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

        orderDetail = PurchaseOrderDetail.builder()
                .id(1L)
                .product(product)
                .quantity(BigDecimal.TEN)
                .quantityReceived(BigDecimal.ZERO)
                .unitPrice(BigDecimal.valueOf(100))
                .discountPercentage(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .build();

        order = PurchaseOrder.builder()
                .id(1L).orderNumber("PO-00001")
                .supplier(supplier).warehouse(warehouse)
                .orderDate(LocalDate.now())
                .status(PurchaseOrder.OrderStatus.CONFIRMADA)
                .paymentTerm(Supplier.PaymentTerm.NET_30)
                .subtotal(BigDecimal.valueOf(1000))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.valueOf(210))
                .total(BigDecimal.valueOf(1210))
                .createdBy(user)
                .build();
        order.addDetail(orderDetail);
    }

    // -- findAll() --

    @Test
    void findAll_ShouldReturnList() {
        GoodsReceipt receipt = GoodsReceipt.builder()
                .id(1L).receiptNumber("GR-00001")
                .purchaseOrder(order)
                .receiptDate(LocalDate.now())
                .createdBy(user)
                .build();

        when(goodsReceiptRepository.findAll()).thenReturn(List.of(receipt));

        List<GoodsReceiptResponse> result = goodsReceiptService.findAll();

        assertEquals(1, result.size());
        assertEquals("GR-00001", result.get(0).getReceiptNumber());
    }

    // -- findById() --

    @Test
    void findById_WhenExists_ShouldReturnReceipt() {
        GoodsReceipt receipt = GoodsReceipt.builder()
                .id(1L).receiptNumber("GR-00001")
                .purchaseOrder(order)
                .receiptDate(LocalDate.now())
                .createdBy(user)
                .build();

        when(goodsReceiptRepository.findById(1L)).thenReturn(Optional.of(receipt));

        GoodsReceiptResponse result = goodsReceiptService.findById(1L);

        assertEquals("GR-00001", result.getReceiptNumber());
    }

    @Test
    void findById_WhenNotExists_ShouldThrow() {
        when(goodsReceiptRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> goodsReceiptService.findById(99L));
    }

    // -- findByPurchaseOrder() --

    @Test
    void findByPurchaseOrder_ShouldReturnList() {
        GoodsReceipt receipt = GoodsReceipt.builder()
                .id(1L).receiptNumber("GR-00001")
                .purchaseOrder(order)
                .receiptDate(LocalDate.now())
                .createdBy(user)
                .build();

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(goodsReceiptRepository.findByPurchaseOrder(order)).thenReturn(List.of(receipt));

        List<GoodsReceiptResponse> result = goodsReceiptService.findByPurchaseOrder(1L);

        assertEquals(1, result.size());
        assertEquals("GR-00001", result.get(0).getReceiptNumber());
    }

    // -- create() validation --

    @Test
    void create_WithBorradorOrder_ShouldThrow() {
        order.setStatus(PurchaseOrder.OrderStatus.BORRADOR);
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        GoodsReceiptRequest request = buildRequest(10, 10, 0, 0, 95);
        assertThrows(BadRequestException.class,
                () -> goodsReceiptService.create(request, user));
    }

    @Test
    void create_WithCancelledOrder_ShouldThrow() {
        order.setStatus(PurchaseOrder.OrderStatus.CANCELADA);
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        GoodsReceiptRequest request = buildRequest(10, 10, 0, 0, 95);
        assertThrows(BadRequestException.class,
                () -> goodsReceiptService.create(request, user));
    }

    @Test
    void create_WithAlreadyCompleteOrder_ShouldThrow() {
        order.setStatus(PurchaseOrder.OrderStatus.RECIBIDA_COMPLETA);
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        GoodsReceiptRequest request = buildRequest(10, 10, 0, 0, 95);
        assertThrows(BadRequestException.class,
                () -> goodsReceiptService.create(request, user));
    }

    @Test
    void create_WithQuantityMismatch_ShouldThrow() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderDetailRepository.findById(1L)).thenReturn(Optional.of(orderDetail));

        // received(5) + damaged(1) + missing(1) = 7 != expected(10)
        GoodsReceiptRequest request = buildRequest(10, 5, 1, 1, 95);
        assertThrows(BadRequestException.class,
                () -> goodsReceiptService.create(request, user));
    }

    @Test
    void create_WithExceedingPendingQuantity_ShouldThrow() {
        orderDetail.setQuantityReceived(BigDecimal.valueOf(8)); // pendiente = 2
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderDetailRepository.findById(1L)).thenReturn(Optional.of(orderDetail));

        // expected=3, received=3, total=3 > pending=2
        GoodsReceiptRequest request = buildRequest(3, 3, 0, 0, 95);
        assertThrows(BadRequestException.class,
                () -> goodsReceiptService.create(request, user));
    }

    // -- create() success scenarios --

    @Test
    void create_WithPartialReceipt_ShouldSetStatusRecibidaParcial() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderDetailRepository.findById(1L)).thenReturn(Optional.of(orderDetail));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(goodsReceiptRepository.getNextReceiptNumber("GR-%")).thenReturn(0);
        when(goodsReceiptRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(purchaseOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // received(5) + damaged(0) + missing(5) = 10 = expected(10) -> valid, but partial
        GoodsReceiptRequest request = buildRequest(10, 5, 0, 5, 95);
        GoodsReceiptResponse result = goodsReceiptService.create(request, user);

        assertNotNull(result);
        verify(purchaseOrderRepository).save(orderCaptor.capture());
        assertEquals(PurchaseOrder.OrderStatus.RECIBIDA_PARCIAL,
                orderCaptor.getValue().getStatus());
        verify(stockMovementService).entryByPurchase(any(StockMovementRequest.class), eq(user));
    }

    @Test
    void create_WithCompleteReceipt_ShouldSetStatusRecibidaCompleta() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderDetailRepository.findById(1L)).thenReturn(Optional.of(orderDetail));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(goodsReceiptRepository.getNextReceiptNumber("GR-%")).thenReturn(0);
        when(goodsReceiptRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(purchaseOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        GoodsReceiptRequest request = buildRequest(10, 10, 0, 0, 95);
        GoodsReceiptResponse result = goodsReceiptService.create(request, user);

        assertNotNull(result);
        verify(purchaseOrderRepository).save(orderCaptor.capture());
        assertEquals(PurchaseOrder.OrderStatus.RECIBIDA_COMPLETA,
                orderCaptor.getValue().getStatus());
        assertNotNull(orderCaptor.getValue().getDeliveryDate());
        verify(stockMovementService).entryByPurchase(any(StockMovementRequest.class), eq(user));
    }

    @Test
    void create_ShouldUpdateQuantityReceivedOnDetail() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderDetailRepository.findById(1L)).thenReturn(Optional.of(orderDetail));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(goodsReceiptRepository.getNextReceiptNumber("GR-%")).thenReturn(0);
        when(goodsReceiptRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(purchaseOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        GoodsReceiptRequest request = buildRequest(10, 10, 0, 0, 95);
        goodsReceiptService.create(request, user);

        assertEquals(BigDecimal.TEN, orderDetail.getQuantityReceived());
    }

    @Test
    void create_WithDamagedGoods_ShouldOnlyAddAcceptedToStock() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderDetailRepository.findById(1L)).thenReturn(Optional.of(orderDetail));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(goodsReceiptRepository.getNextReceiptNumber("GR-%")).thenReturn(0);
        when(goodsReceiptRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(purchaseOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // expected=10, received=8, damaged=2, missing=0 → total=10=expected ✔
        // accepted = received - damaged = 8 - 2 = 6
        GoodsReceiptRequest request = buildRequest(10, 8, 2, 0, 95);
        goodsReceiptService.create(request, user);

        ArgumentCaptor<StockMovementRequest> movementCaptor =
                ArgumentCaptor.forClass(StockMovementRequest.class);
        verify(stockMovementService).entryByPurchase(movementCaptor.capture(), eq(user));
        assertEquals(BigDecimal.valueOf(6), movementCaptor.getValue().getQuantity());
    }

    @Test
    void create_WithZeroAccepted_ShouldNotCreateStockMovement() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderDetailRepository.findById(1L)).thenReturn(Optional.of(orderDetail));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(goodsReceiptRepository.getNextReceiptNumber("GR-%")).thenReturn(0);
        when(goodsReceiptRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(purchaseOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // expected=10, received=5, damaged=5, missing=0 → total=10=expected ✔
        // accepted = 5 - 5 = 0 → no stock movement
        GoodsReceiptRequest request = buildRequest(10, 5, 5, 0, 95);
        goodsReceiptService.create(request, user);

        verify(stockMovementService, never())
                .entryByPurchase(any(StockMovementRequest.class), any());
    }

    @Test
    void create_ShouldGenerateReceiptNumber() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderDetailRepository.findById(1L)).thenReturn(Optional.of(orderDetail));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(goodsReceiptRepository.getNextReceiptNumber("GR-%")).thenReturn(0);
        when(goodsReceiptRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(purchaseOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        GoodsReceiptRequest request = buildRequest(10, 10, 0, 0, 95);
        GoodsReceiptResponse result = goodsReceiptService.create(request, user);

        assertEquals("GR-00001", result.getReceiptNumber());
    }

    @Test
    void create_WithPartialReceiptShouldNotSetDeliveryDate() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderDetailRepository.findById(1L)).thenReturn(Optional.of(orderDetail));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(goodsReceiptRepository.getNextReceiptNumber("GR-%")).thenReturn(0);
        when(goodsReceiptRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(purchaseOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        GoodsReceiptRequest request = buildRequest(10, 5, 0, 5, 95);
        goodsReceiptService.create(request, user);

        verify(purchaseOrderRepository).save(orderCaptor.capture());
        assertNull(orderCaptor.getValue().getDeliveryDate());
    }

    // -- Helper: build a request with specific quantities --

    private GoodsReceiptRequest buildRequest(int expectedQty, int receivedQty,
                                              int damagedQty, int missingQty,
                                              int unitCost) {
        return GoodsReceiptRequest.builder()
                .purchaseOrderId(1L)
                .receiptDate(LocalDate.now())
                .notes("Recepción de prueba")
                .details(List.of(
                        GoodsReceiptRequest.GoodsReceiptDetailRequest.builder()
                                .purchaseOrderDetailId(1L)
                                .productId(1L)
                                .expectedQuantity(BigDecimal.valueOf(expectedQty))
                                .receivedQuantity(BigDecimal.valueOf(receivedQty))
                                .damagedQuantity(BigDecimal.valueOf(damagedQty))
                                .missingQuantity(BigDecimal.valueOf(missingQty))
                                .unitCost(BigDecimal.valueOf(unitCost))
                                .batchNumber("LOTE-001")
                                .expirationDate(LocalDate.now().plusMonths(6))
                                .build()
                ))
                .build();
    }
}
