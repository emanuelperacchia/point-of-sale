package com.pos.system.service;

import com.pos.system.dto.request.CreateReturnRequest;
import com.pos.system.dto.response.SaleReturnResponse;
import com.pos.system.entity.*;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.*;
import com.pos.system.service.impl.SaleReturnServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleReturnServiceTest {

    @Mock private SaleReturnRepository saleReturnRepository;
    @Mock private ReturnItemRepository returnItemRepository;
    @Mock private SaleRepository saleRepository;
    @Mock private StockMovementService stockMovementService;
    @Mock private UserRepository userRepository;

    private SaleReturnServiceImpl saleReturnService;

    private Sale sale;
    private SaleItem saleItem;
    private Product product;
    private Warehouse warehouse;
    private User user;

    @BeforeEach
    void setUp() {
        saleReturnService = new SaleReturnServiceImpl(
                saleReturnRepository, returnItemRepository, saleRepository,
                stockMovementService, userRepository);
        ReflectionTestUtils.setField(saleReturnService, "autoApproveLimit", BigDecimal.valueOf(5000));

        product = Product.builder().id(1L).name("Producto Test").build();
        warehouse = Warehouse.builder().id(1L).name("Bodega Principal").build();

        saleItem = SaleItem.builder()
                .id(1L).product(product)
                .quantity(5).unitPrice(BigDecimal.valueOf(1000))
                .build();

        sale = Sale.builder()
                .id(1L).warehouse(warehouse)
                .items(List.of(saleItem))
                .payments(List.of(
                        Payment.builder().paymentMethod(PaymentMethod.CASH)
                                .amount(BigDecimal.valueOf(5000)).build()
                ))
                .build();

        user = User.builder().id(1L).firstName("Test").lastName("User").build();
    }

    @Test
    void createReturn_WithAmountBelowLimit_ShouldAutoApprove() {
        CreateReturnRequest request = new CreateReturnRequest();
        request.setSaleId(1L);
        request.setMotivo("Producto defectuoso");
        CreateReturnRequest.ReturnItemRequest itemReq = new CreateReturnRequest.ReturnItemRequest();
        itemReq.setSaleItemId(1L);
        itemReq.setCantidad(2);
        request.setItems(List.of(itemReq));

        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(saleReturnRepository.save(any(SaleReturn.class)))
                .thenAnswer(invocation -> {
                    SaleReturn sr = invocation.getArgument(0);
                    sr.setId(1L);
                    return sr;
                });
        when(returnItemRepository.saveAll(anyList())).thenReturn(List.of());
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(stockMovementService.entryByReturn(any(), any())).thenReturn(null);

        SaleReturnResponse response = saleReturnService.createReturn(request, 1L);

        assertNotNull(response);
        assertEquals(ReturnStatus.APROBADA, response.estado());
        assertEquals(0, BigDecimal.valueOf(2000).compareTo(response.montoTotal()));
        assertEquals(PaymentMethod.CASH, response.metodoDevolucion());
        assertEquals(1, response.items().size());

        verify(stockMovementService, times(1)).entryByReturn(any(), any());
    }

    @Test
    void createReturn_WithAmountAboveLimit_ShouldRequireApproval() {
        // Hacer el item más caro para superar el límite con pocas unidades
        ReflectionTestUtils.setField(saleReturnService, "autoApproveLimit", BigDecimal.valueOf(500));
        saleItem.setUnitPrice(BigDecimal.valueOf(1000));

        CreateReturnRequest request = new CreateReturnRequest();
        request.setSaleId(1L);
        request.setMotivo("Devolución grande");
        CreateReturnRequest.ReturnItemRequest itemReq = new CreateReturnRequest.ReturnItemRequest();
        itemReq.setSaleItemId(1L);
        itemReq.setCantidad(3);
        request.setItems(List.of(itemReq));

        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(saleReturnRepository.save(any(SaleReturn.class)))
                .thenAnswer(invocation -> {
                    SaleReturn sr = invocation.getArgument(0);
                    sr.setId(2L);
                    return sr;
                });
        when(returnItemRepository.saveAll(anyList())).thenReturn(List.of());

        SaleReturnResponse response = saleReturnService.createReturn(request, 1L);

        assertNotNull(response);
        assertEquals(ReturnStatus.PENDIENTE_APROBACION, response.estado());
        assertEquals(0, BigDecimal.valueOf(3000).compareTo(response.montoTotal()));
        // No debe reintegrar stock porque está pendiente
        verify(stockMovementService, never()).entryByReturn(any(), any());
    }

    @Test
    void createReturn_WhenQuantityExceedsSold_ShouldThrow() {
        CreateReturnRequest request = new CreateReturnRequest();
        request.setSaleId(1L);
        request.setMotivo("Excede cantidad");
        CreateReturnRequest.ReturnItemRequest itemReq = new CreateReturnRequest.ReturnItemRequest();
        itemReq.setSaleItemId(1L);
        itemReq.setCantidad(10); // Only 5 were sold
        request.setItems(List.of(itemReq));

        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));

        assertThrows(BadRequestException.class,
                () -> saleReturnService.createReturn(request, 1L));
        verify(saleReturnRepository, never()).save(any());
    }

    @Test
    void createReturn_WhenItemNotInSale_ShouldThrow() {
        CreateReturnRequest request = new CreateReturnRequest();
        request.setSaleId(1L);
        request.setMotivo("Item inválido");
        CreateReturnRequest.ReturnItemRequest itemReq = new CreateReturnRequest.ReturnItemRequest();
        itemReq.setSaleItemId(99L); // Doesn't belong to sale
        itemReq.setCantidad(1);
        request.setItems(List.of(itemReq));

        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));

        assertThrows(BadRequestException.class,
                () -> saleReturnService.createReturn(request, 1L));
    }

    @Test
    void approveReturn_ShouldReintegrateStock() {
        SaleReturn pendingReturn = SaleReturn.builder()
                .id(1L).saleId(1L).estado(ReturnStatus.PENDIENTE_APROBACION)
                .montoTotal(BigDecimal.valueOf(2000)).motivo("Revisión")
                .metodoDevolucion(PaymentMethod.CASH)
                .build();

        ReturnItem returnItem = ReturnItem.builder()
                .id(1L).returnId(1L).saleItemId(1L).cantidad(2).precioUnitario(BigDecimal.valueOf(1000))
                .build();

        when(saleReturnRepository.findById(1L)).thenReturn(Optional.of(pendingReturn));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(returnItemRepository.findByReturnId(1L)).thenReturn(List.of(returnItem));
        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(stockMovementService.entryByReturn(any(), any())).thenReturn(null);
        when(saleReturnRepository.save(any(SaleReturn.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SaleReturnResponse response = saleReturnService.approveReturn(1L, 2L);

        assertEquals(ReturnStatus.APROBADA, response.estado());
        verify(stockMovementService, times(1)).entryByReturn(any(), any());
    }

    @Test
    void approveReturn_WhenAlreadyApproved_ShouldThrow() {
        SaleReturn approvedReturn = SaleReturn.builder()
                .id(1L).estado(ReturnStatus.APROBADA).build();

        when(saleReturnRepository.findById(1L)).thenReturn(Optional.of(approvedReturn));

        assertThrows(BadRequestException.class,
                () -> saleReturnService.approveReturn(1L, 2L));
        verify(stockMovementService, never()).entryByReturn(any(), any());
    }

    @Test
    void rejectReturn_ShouldSetRejected() {
        SaleReturn pendingReturn = SaleReturn.builder()
                .id(1L).saleId(1L).estado(ReturnStatus.PENDIENTE_APROBACION)
                .montoTotal(BigDecimal.valueOf(2000)).motivo("Revisión")
                .metodoDevolucion(PaymentMethod.CASH)
                .build();

        when(saleReturnRepository.findById(1L)).thenReturn(Optional.of(pendingReturn));
        when(returnItemRepository.findByReturnId(1L)).thenReturn(List.of());
        when(saleReturnRepository.save(any(SaleReturn.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SaleReturnResponse response = saleReturnService.rejectReturn(1L, 2L);

        assertEquals(ReturnStatus.RECHAZADA, response.estado());
        verify(stockMovementService, never()).entryByReturn(any(), any());
    }

    @Test
    void getById_WhenNotFound_ShouldThrow() {
        when(saleReturnRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> saleReturnService.getById(99L));
    }

    @Test
    void findBySaleId_ShouldReturnList() {
        SaleReturn sr = SaleReturn.builder()
                .id(1L).saleId(1L).estado(ReturnStatus.APROBADA)
                .montoTotal(BigDecimal.valueOf(2000)).motivo("Test")
                .metodoDevolucion(PaymentMethod.CASH)
                .build();

        when(saleReturnRepository.findBySaleIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(sr));
        when(returnItemRepository.findByReturnId(1L)).thenReturn(List.of());

        List<SaleReturnResponse> results = saleReturnService.findBySaleId(1L);

        assertEquals(1, results.size());
        assertEquals(ReturnStatus.APROBADA, results.get(0).estado());
    }
}
