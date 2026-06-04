package com.pos.system.service;

import com.pos.system.dto.request.SupplierReturnRequest;
import com.pos.system.dto.response.SupplierReturnResponse;
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
class SupplierReturnServiceTest {

    @Mock private SupplierReturnRepository supplierReturnRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private ProductRepository productRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private StockMovementService stockMovementService;

    @Captor private ArgumentCaptor<SupplierReturn> returnCaptor;
    @Captor private ArgumentCaptor<Supplier> supplierCaptor;

    private SupplierReturnService supplierReturnService;

    private Supplier supplier;
    private Product product;
    private Warehouse warehouse;
    private User user;
    private PurchaseOrder purchaseOrder;
    private SupplierReturnRequest request;

    @BeforeEach
    void setUp() {
        supplierReturnService = new SupplierReturnService(
                supplierReturnRepository, supplierRepository,
                productRepository, warehouseRepository,
                purchaseOrderRepository, stockMovementService);

        supplier = Supplier.builder()
                .id(1L).code("PROV001").taxId("30-12345678-9")
                .businessName("Proveedor S.A.")
                .paymentTerm(Supplier.PaymentTerm.NET_30)
                .creditLimit(BigDecimal.valueOf(100000))
                .currentDebt(BigDecimal.valueOf(50000))
                .active(true)
                .build();

        product = Product.builder()
                .id(1L).name("Producto Test").sku("TST-001")
                .price(BigDecimal.valueOf(100))
                .stock(50)
                .active(true)
                .build();

        warehouse = Warehouse.builder()
                .id(1L).code("MAIN").name("Bodega Principal")
                .active(true)
                .build();

        user = User.builder()
                .id(1L).firstName("Admin").lastName("User")
                .email("admin@test.com")
                .build();

        purchaseOrder = PurchaseOrder.builder()
                .id(1L).orderNumber("PO-00001")
                .supplier(supplier)
                .status(PurchaseOrder.OrderStatus.RECIBIDA_COMPLETA)
                .build();

        request = SupplierReturnRequest.builder()
                .supplierId(1L)
                .purchaseOrderId(1L)
                .productId(1L)
                .quantity(BigDecimal.valueOf(5))
                .unitCost(BigDecimal.valueOf(95))
                .returnDate(LocalDate.now())
                .reason(SupplierReturn.ReturnReason.DEFECTUOSO)
                .warehouseId(1L)
                .notes("Productos con fallas de fabricación")
                .build();
    }

    // -- findAll() --

    @Test
    void findAll_ShouldReturnList() {
        SupplierReturn supplierReturn = buildDefaultReturn();
        when(supplierReturnRepository.findAll()).thenReturn(List.of(supplierReturn));

        List<SupplierReturnResponse> result = supplierReturnService.findAll();

        assertEquals(1, result.size());
        assertEquals("SR-00001", result.get(0).getReturnNumber());
        assertEquals("Proveedor S.A.", result.get(0).getSupplierName());
    }

    // -- findById() --

    @Test
    void findById_WhenExists_ShouldReturnReturn() {
        SupplierReturn supplierReturn = buildDefaultReturn();
        when(supplierReturnRepository.findById(1L)).thenReturn(Optional.of(supplierReturn));

        SupplierReturnResponse result = supplierReturnService.findById(1L);

        assertEquals("SR-00001", result.getReturnNumber());
        assertEquals(SupplierReturn.ReturnReason.DEFECTUOSO.getDescription(), result.getReason());
    }

    @Test
    void findById_WhenNotExists_ShouldThrow() {
        when(supplierReturnRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> supplierReturnService.findById(99L));
    }

    // -- findBySupplier() --

    @Test
    void findBySupplier_ShouldReturnList() {
        SupplierReturn supplierReturn = buildDefaultReturn();
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierReturnRepository.findBySupplier(supplier)).thenReturn(List.of(supplierReturn));

        List<SupplierReturnResponse> result = supplierReturnService.findBySupplier(1L);

        assertEquals(1, result.size());
    }

    @Test
    void findBySupplier_WhenSupplierNotExists_ShouldThrow() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> supplierReturnService.findBySupplier(99L));
    }

    // -- findByStatus() --

    @Test
    void findByStatus_ShouldReturnList() {
        SupplierReturn supplierReturn = buildDefaultReturn();
        when(supplierReturnRepository.findByStatus(SupplierReturn.ReturnStatus.PENDING))
                .thenReturn(List.of(supplierReturn));

        List<SupplierReturnResponse> result =
                supplierReturnService.findByStatus(SupplierReturn.ReturnStatus.PENDING);

        assertEquals(1, result.size());
    }

    // -- create() validations --

    @Test
    void create_WithInsufficientStock_ShouldThrow() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        doThrow(new BadRequestException("Stock insuficiente"))
                .when(stockMovementService)
                .validateStockAvailable(1L, 1L, BigDecimal.valueOf(5));

        assertThrows(BadRequestException.class,
                () -> supplierReturnService.create(request, user));
        verify(purchaseOrderRepository, never()).findById(any());
    }

    // -- create() success scenarios --

    @Test
    void create_WithValidData_ShouldSucceed() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));
        when(supplierReturnRepository.getNextReturnNumber("SR-%")).thenReturn(0);
        when(supplierReturnRepository.getNextCreditNoteNumber("CN-%")).thenReturn(0);
        when(supplierReturnRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(supplierRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SupplierReturnResponse result = supplierReturnService.create(request, user);

        assertNotNull(result);
        assertEquals("SR-00001", result.getReturnNumber());
        assertEquals(SupplierReturn.ReturnStatus.COMPLETED.getDescription(), result.getStatus());
        assertNotNull(result.getCreditNoteNumber());
        verify(stockMovementService).exitBySupplierReturn(any(StockMovementRequest.class), eq(user));
    }

    @Test
    void create_ShouldCalculateSubtotalCorrectly() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));
        when(supplierReturnRepository.getNextReturnNumber("SR-%")).thenReturn(0);
        when(supplierReturnRepository.getNextCreditNoteNumber("CN-%")).thenReturn(0);
        when(supplierReturnRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(supplierRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        request.setQuantity(BigDecimal.valueOf(10));
        request.setUnitCost(BigDecimal.valueOf(100));

        SupplierReturnResponse result = supplierReturnService.create(request, user);

        assertEquals(0, BigDecimal.valueOf(1000).compareTo(result.getSubtotal()));
        assertEquals(0, BigDecimal.valueOf(1000).compareTo(result.getCreditNoteAmount()));
    }

    @Test
    void create_ShouldGenerateCreditNote() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));
        when(supplierReturnRepository.getNextReturnNumber("SR-%")).thenReturn(0);
        when(supplierReturnRepository.getNextCreditNoteNumber("CN-%")).thenReturn(0);
        when(supplierReturnRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(supplierRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SupplierReturnResponse result = supplierReturnService.create(request, user);

        assertNotNull(result.getCreditNoteNumber());
        assertTrue(result.getCreditNoteNumber().startsWith("CN-"));
    }

    @Test
    void create_ShouldUpdateSupplierDebt() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));
        when(supplierReturnRepository.getNextReturnNumber("SR-%")).thenReturn(0);
        when(supplierReturnRepository.getNextCreditNoteNumber("CN-%")).thenReturn(0);
        when(supplierReturnRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(supplierRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        supplierReturnService.create(request, user);

        verify(supplierRepository).save(supplierCaptor.capture());
        Supplier savedSupplier = supplierCaptor.getValue();
        // currentDebt = 50000, subtotal = 5 * 95 = 475, newDebt = 50000 - 475 = 49525
        assertEquals(0, BigDecimal.valueOf(49525).compareTo(savedSupplier.getCurrentDebt()));
    }

    @Test
    void create_ShouldNotMakeDebtNegative() {
        supplier.setCurrentDebt(BigDecimal.valueOf(100));

        request.setQuantity(BigDecimal.valueOf(10));
        request.setUnitCost(BigDecimal.valueOf(50));

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));
        when(supplierReturnRepository.getNextReturnNumber("SR-%")).thenReturn(0);
        when(supplierReturnRepository.getNextCreditNoteNumber("CN-%")).thenReturn(0);
        when(supplierReturnRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(supplierRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        supplierReturnService.create(request, user);

        verify(supplierRepository).save(supplierCaptor.capture());
        assertEquals(0, BigDecimal.ZERO.compareTo(supplierCaptor.getValue().getCurrentDebt()));
    }

    @Test
    void create_WithoutPurchaseOrder_ShouldWork() {
        request.setPurchaseOrderId(null);

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(supplierReturnRepository.getNextReturnNumber("SR-%")).thenReturn(0);
        when(supplierReturnRepository.getNextCreditNoteNumber("CN-%")).thenReturn(0);
        when(supplierReturnRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(supplierRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SupplierReturnResponse result = supplierReturnService.create(request, user);

        assertNotNull(result);
        assertNull(result.getPurchaseOrderId());
        assertNull(result.getPurchaseOrderNumber());
        verify(purchaseOrderRepository, never()).findById(any());
        verify(stockMovementService).exitBySupplierReturn(any(StockMovementRequest.class), eq(user));
    }

    @Test
    void create_ShouldExitStockWithCorrectQuantity() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));
        when(supplierReturnRepository.getNextReturnNumber("SR-%")).thenReturn(0);
        when(supplierReturnRepository.getNextCreditNoteNumber("CN-%")).thenReturn(0);
        when(supplierReturnRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(supplierRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        supplierReturnService.create(request, user);

        ArgumentCaptor<StockMovementRequest> movementCaptor =
                ArgumentCaptor.forClass(StockMovementRequest.class);
        verify(stockMovementService).exitBySupplierReturn(movementCaptor.capture(), eq(user));

        StockMovementRequest movement = movementCaptor.getValue();
        assertEquals(1L, movement.getProductId());
        assertEquals(1L, movement.getWarehouseId());
        assertEquals(BigDecimal.valueOf(5), movement.getQuantity());
        assertEquals(BigDecimal.valueOf(95), movement.getUnitCost());
    }

    // -- Helper --

    private SupplierReturn buildDefaultReturn() {
        return SupplierReturn.builder()
                .id(1L)
                .returnNumber("SR-00001")
                .supplier(supplier)
                .product(product)
                .quantity(BigDecimal.valueOf(5))
                .unitCost(BigDecimal.valueOf(95))
                .subtotal(BigDecimal.valueOf(475))
                .returnDate(LocalDate.now())
                .reason(SupplierReturn.ReturnReason.DEFECTUOSO)
                .status(SupplierReturn.ReturnStatus.COMPLETED)
                .creditNoteNumber("CN-00001")
                .creditNoteAmount(BigDecimal.valueOf(475))
                .warehouse(warehouse)
                .createdBy(user)
                .purchaseOrder(purchaseOrder)
                .build();
    }
}
