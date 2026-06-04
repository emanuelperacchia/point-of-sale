package com.pos.system.service;

import com.pos.system.dto.request.CartValidationRequest;
import com.pos.system.dto.request.SaleRequest;
import com.pos.system.dto.request.StockMovementRequest;
import com.pos.system.dto.response.CartValidationResponse;
import com.pos.system.dto.response.SaleResponse;
import com.pos.system.entity.*;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.*;
import com.pos.system.service.impl.SaleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock private SaleRepository saleRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private UserRepository userRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private ProductStockRepository productStockRepository;
    @Mock private StockMovementService stockMovementService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private CashShiftRepository cashShiftRepository;

    private SaleService saleService;

    private User user;
    private Warehouse warehouse;
    private Product product;
    private ProductStock productStock;

    @BeforeEach
    void setUp() {
        saleService = new SaleServiceImpl(
                saleRepository, productRepository, clientRepository,
                userRepository, warehouseRepository, productStockRepository,
                stockMovementService, cashShiftRepository, eventPublisher
        );

        user = User.builder().id(1L).firstName("Admin").lastName("User").email("admin@test.com").build();
        warehouse = Warehouse.builder().id(1L).code("MAIN").name("Bodega Principal").active(true).build();
        product = Product.builder()
                .id(1L).name("Sándwich de Miga").sku("SM-001")
                .price(BigDecimal.valueOf(1500))
                .stock(100)
                .active(true)
                .build();
        productStock = ProductStock.builder()
                .id(1L).product(product).warehouse(warehouse)
                .currentStock(BigDecimal.valueOf(50))
                .reservedStock(BigDecimal.ZERO)
                .build();
    }

    // =====================
    // processSale tests
    // =====================

    @Test
    void processSale_WithValidData_ShouldCreateSaleAndDeductStock() {
        // Given
        SaleRequest request = buildSimpleSaleRequest();
        stubBasicRepositories();
        stubProductStock(50);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale s = invocation.getArgument(0);
            s.setId(1L);
            return s;
        });

        // When
        SaleResponse result = saleService.processSale(request, 1L);

        // Then
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(0, BigDecimal.valueOf(3000).compareTo(result.getTotal()));
        assertEquals(1, result.getItems().size());
        assertEquals("Sándwich de Miga", result.getItems().get(0).getProductName());
        assertEquals(1, result.getPayments().size());
        assertEquals("CASH", result.getPayments().get(0).getPaymentMethod());
        verify(stockMovementService).exitBySale(any(StockMovementRequest.class), eq(user));
    }

    @Test
    void processSale_WithClient_ShouldAssociateClient() {
        // Given
        Client client = Client.builder().id(1L).name("Juan Pérez").documentNumber("12.345.678-9").build();
        SaleRequest request = buildSimpleSaleRequest();
        request.setClientId(1L);

        stubBasicRepositories();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        stubProductStock(50);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SaleResponse result = saleService.processSale(request, 1L);

        // Then
        assertNotNull(result.getClient());
        assertEquals("Juan Pérez", result.getClient().getName());
    }

    @Test
    void processSale_WhenProductNotFound_ShouldThrowException() {
        SaleRequest request = buildSimpleSaleRequest();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> saleService.processSale(request, 1L));
        verifyNoInteractions(saleRepository);
    }

    @Test
    void processSale_WhenInsufficientStock_ShouldThrowException() {
        SaleRequest request = buildSimpleSaleRequest();

        stubBasicRepositories();
        stubProductStock(0); // available = 0, requested = 2

        assertThrows(BadRequestException.class, () -> saleService.processSale(request, 1L));
        verifyNoInteractions(saleRepository);
    }

    @Test
    void processSale_WhenPaymentTotalMismatch_ShouldThrowException() {
        SaleRequest request = buildSimpleSaleRequest();
        request.getPayments().get(0).setAmount(BigDecimal.valueOf(2500));

        stubBasicRepositories();
        stubProductStock(50);

        assertThrows(BadRequestException.class, () -> saleService.processSale(request, 1L));
        verifyNoInteractions(saleRepository);
    }

    @Test
    void processSale_WithDiscount_ShouldCalculateCorrectTotal() {
        // Product price = 1500, qty = 2 → subtotal = 3000, discount = 200, no tax → total = 2800
        SaleRequest request = buildSimpleSaleRequest();
        request.getItems().get(0).setDiscount(BigDecimal.valueOf(200));
        request.getPayments().get(0).setAmount(BigDecimal.valueOf(2800));

        stubBasicRepositories();
        stubProductStock(50);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SaleResponse result = saleService.processSale(request, 1L);

        assertEquals(0, BigDecimal.valueOf(2800).compareTo(result.getTotal()));
        assertEquals(0, BigDecimal.valueOf(3000).compareTo(result.getSubtotal()));
        assertEquals(0, BigDecimal.valueOf(200).compareTo(result.getDiscount()));
    }

    @Test
    void processSale_WithInvalidPaymentMethod_ShouldThrowException() {
        SaleRequest request = buildSimpleSaleRequest();
        request.getPayments().get(0).setPaymentMethod("BITCOIN");

        stubBasicRepositories();
        stubProductStock(50);

        assertThrows(BadRequestException.class, () -> saleService.processSale(request, 1L));
        verifyNoInteractions(saleRepository);
    }

    // =====================
    // getById tests
    // =====================

    @Test
    void getById_WhenSaleExists_ShouldReturnSale() {
        Sale sale = Sale.builder()
                .id(1L).user(user).warehouse(warehouse)
                .status(SaleStatus.COMPLETED)
                .subtotal(BigDecimal.valueOf(3000))
                .taxAmount(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .total(BigDecimal.valueOf(3000))
                .build();

        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));

        SaleResponse result = saleService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getById_WhenSaleNotFound_ShouldThrowException() {
        when(saleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> saleService.getById(99L));
    }

    // =====================
    // validateCart tests
    // =====================

    @Test
    void validateCart_WhenStockSufficient_ShouldReturnValid() {
        CartValidationRequest request = new CartValidationRequest();
        request.setWarehouseId(1L);
        request.setItems(List.of(createCartItem(1L, 2)));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        stubProductStockForValidation(50);

        CartValidationResponse result = saleService.validateCart(request);

        assertTrue(result.isValid());
        assertTrue(result.getItems().get(0).isEnoughStock());
    }

    @Test
    void validateCart_WhenStockInsufficient_ShouldReturnInvalid() {
        CartValidationRequest request = new CartValidationRequest();
        request.setWarehouseId(1L);
        request.setItems(List.of(createCartItem(1L, 999)));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        stubProductStockForValidation(50);

        CartValidationResponse result = saleService.validateCart(request);

        assertFalse(result.isValid());
        assertFalse(result.getItems().get(0).isEnoughStock());
    }

    @Test
    void validateCart_WhenProductNotFound_ShouldReturnInvalid() {
        CartValidationRequest request = new CartValidationRequest();
        request.setWarehouseId(1L);
        request.setItems(List.of(createCartItem(99L, 1)));

        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        CartValidationResponse result = saleService.validateCart(request);

        assertFalse(result.isValid());
        assertEquals("Producto no encontrado", result.getItems().get(0).getProductName());
    }

    // =====================
    // helpers
    // =====================

    private void stubBasicRepositories() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(product));
        // getReferenceById() returns proxy — stub both findById and getReferenceById
        lenient().when(productRepository.getReferenceById(1L)).thenReturn(product);
        lenient().when(warehouseRepository.getReferenceById(1L)).thenReturn(warehouse);
    }

    /**
     * Stubs productStockRepository for use by getAvailableStock() inside processSale.
     * getAvailableStock() calls getReferenceById() internally which returns a different
     * object reference, so we must use any() matchers.
     */
    private void stubProductStock(int availableStock) {
        productStock.setCurrentStock(BigDecimal.valueOf(availableStock));
        lenient().when(productStockRepository.findByProductAndWarehouse(any(), any()))
                .thenReturn(Optional.of(productStock));
    }

    /**
     * Stubs productStockRepository for validateCart tests.
     * validateCart() calls productRepository.findById() (not getReferenceById),
     * so the product object passed to findByProductAndWarehouse is the same
     * as the one we mock. But we still use any() for safety.
     */
    private void stubProductStockForValidation(int availableStock) {
        productStock.setCurrentStock(BigDecimal.valueOf(availableStock));
        when(productStockRepository.findByProductAndWarehouse(any(), any()))
                .thenReturn(Optional.of(productStock));
    }

    private SaleRequest buildSimpleSaleRequest() {
        SaleRequest request = new SaleRequest();

        SaleRequest.SaleItemRequest item = new SaleRequest.SaleItemRequest();
        item.setProductId(1L);
        item.setQuantity(2);
        item.setDiscount(BigDecimal.ZERO);
        request.setItems(List.of(item));

        SaleRequest.PaymentRequest payment = new SaleRequest.PaymentRequest();
        payment.setPaymentMethod("CASH");
        payment.setAmount(BigDecimal.valueOf(3000));
        request.setPayments(List.of(payment));

        return request;
    }

    private CartValidationRequest.CartItem createCartItem(Long productId, int quantity) {
        CartValidationRequest.CartItem item = new CartValidationRequest.CartItem();
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }
}
