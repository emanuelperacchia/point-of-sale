package com.pos.system.service;

import com.pos.system.dto.request.PayablePaymentRequest;
import com.pos.system.dto.request.PayableRequest;
import com.pos.system.dto.response.PayablePaymentResponse;
import com.pos.system.dto.response.PayableResponse;
import com.pos.system.entity.Payable;
import com.pos.system.entity.PayablePayment;
import com.pos.system.entity.PaymentMethod;
import com.pos.system.entity.Supplier;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.PayablePaymentRepository;
import com.pos.system.repository.PayableRepository;
import com.pos.system.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayableServiceTest {

    @Mock private PayableRepository payableRepository;
    @Mock private PayablePaymentRepository paymentRepository;
    @Mock private SupplierRepository supplierRepository;

    private PayableService payableService;

    private Payable payable;
    private Supplier supplier;
    private final LocalDate today = LocalDate.of(2026, 6, 8);

    @BeforeEach
    void setUp() {
        payableService = new PayableService(payableRepository, paymentRepository, supplierRepository);

        supplier = Supplier.builder()
                .id(1L).code("PROV001").taxId("30-12345678-9")
                .businessName("Proveedor S.A.")
                .build();

        payable = Payable.builder()
                .id(100L)
                .supplierId(1L)
                .purchaseOrderId(50L)
                .montoOriginal(BigDecimal.valueOf(100000))
                .saldoPendiente(BigDecimal.valueOf(100000))
                .fechaEmision(today.minusDays(5))
                .fechaVencimiento(today.plusDays(25))
                .estado(Payable.Estado.PENDIENTE)
                .referenciaBancaria(null)
                .build();
    }

    // ── createPayable ──────────────────────────────────────────────────

    @Test
    void createPayable_ShouldCreateAndReturn() {
        // Given
        PayableRequest request = PayableRequest.builder()
                .supplierId(1L)
                .montoOriginal(BigDecimal.valueOf(50000))
                .fechaEmision(today)
                .fechaVencimiento(today.plusDays(30))
                .build();

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(payableRepository.save(any())).thenAnswer(invocation -> {
            Payable saved = invocation.getArgument(0);
            saved.setId(200L);
            return saved;
        });

        // When
        PayableResponse result = payableService.createPayable(request);

        // Then
        assertNotNull(result);
        assertEquals(200L, result.getId());
        assertEquals(BigDecimal.valueOf(50000), result.getMontoOriginal());
        assertEquals("PENDIENTE", result.getEstado());
        verify(payableRepository).save(any());
    }

    @Test
    void createPayable_WhenSupplierNotFound_ShouldThrow() {
        // Given
        PayableRequest request = PayableRequest.builder()
                .supplierId(999L)
                .montoOriginal(BigDecimal.valueOf(1000))
                .fechaEmision(today)
                .fechaVencimiento(today.plusDays(30))
                .build();

        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(ResourceNotFoundException.class,
                () -> payableService.createPayable(request));
    }

    // ── findByFilters ──────────────────────────────────────────────────

    @Test
    void findByFilters_ShouldReturnFilteredResults() {
        // Given
        when(payableRepository.findByFilters(isNull(), isNull(), isNull()))
                .thenReturn(List.of(payable));

        // When
        var page = payableService.findByFilters(null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 20));

        // Then
        assertEquals(1, page.getTotalElements());
        assertEquals(100L, page.getContent().get(0).getId());
    }

    // ── getById ────────────────────────────────────────────────────────

    @Test
    void getById_WhenExists_ShouldReturn() {
        // Given
        when(payableRepository.findById(100L)).thenReturn(Optional.of(payable));
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        // When
        PayableResponse result = payableService.getById(100L);

        // Then
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("Proveedor S.A.", result.getSupplierName());
    }

    @Test
    void getById_WhenNotFound_ShouldThrow() {
        when(payableRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> payableService.getById(999L));
    }

    // ── getPayments ────────────────────────────────────────────────────

    @Test
    void getPayments_ShouldReturnList() {
        // Given
        PayablePayment payment = PayablePayment.builder()
                .id(1L).payableId(100L)
                .monto(BigDecimal.valueOf(50000))
                .metodoPago(PaymentMethod.TRANSFER)
                .fecha(LocalDateTime.now()).registradoPor(1L)
                .build();
        when(paymentRepository.findByPayableId(100L)).thenReturn(List.of(payment));

        // When
        List<PayablePaymentResponse> result = payableService.getPayments(100L);

        // Then
        assertEquals(1, result.size());
        assertEquals(BigDecimal.valueOf(50000), result.get(0).getMonto());
    }

    // ── getUpcoming ────────────────────────────────────────────────────

    @Test
    void getUpcoming_ShouldReturnPayablesWithinWindow() {
        // Given
        when(payableRepository.findProximosVencer(any(), any()))
                .thenReturn(List.of(payable));
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        // When
        List<PayableResponse> result = payableService.getUpcoming(30);

        // Then
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getId());
    }

    // ── registrarPago ──────────────────────────────────────────────────

    @Test
    void registrarPago_WithFullPayment_ShouldMarkPagada() {
        // Given
        when(payableRepository.findById(100L)).thenReturn(Optional.of(payable));
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        PayablePaymentRequest request = PayablePaymentRequest.builder()
                .monto(BigDecimal.valueOf(100000))
                .metodoPago("TRANSFER")
                .build();

        // When
        PayableResponse result = payableService.registrarPago(100L, request, 1L);

        // Then
        assertEquals("PAGADA", result.getEstado());
        assertEquals(BigDecimal.ZERO, result.getSaldoPendiente());
        verify(paymentRepository).save(any());
        verify(payableRepository).save(any());
    }

    @Test
    void registrarPago_WithPartialPayment_ShouldMarkParcial() {
        // Given
        when(payableRepository.findById(100L)).thenReturn(Optional.of(payable));
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        PayablePaymentRequest request = PayablePaymentRequest.builder()
                .monto(BigDecimal.valueOf(40000))
                .metodoPago("CASH")
                .build();

        // When
        PayableResponse result = payableService.registrarPago(100L, request, 1L);

        // Then
        assertEquals("PARCIAL", result.getEstado());
        assertEquals(BigDecimal.valueOf(60000), result.getSaldoPendiente());
    }

    @Test
    void registrarPago_WhenAlreadyPagada_ShouldThrow() {
        // Given
        payable.setEstado(Payable.Estado.PAGADA);
        when(payableRepository.findById(100L)).thenReturn(Optional.of(payable));

        PayablePaymentRequest request = PayablePaymentRequest.builder()
                .monto(BigDecimal.valueOf(1000))
                .metodoPago("CASH")
                .build();

        // When / Then
        assertThrows(BadRequestException.class,
                () -> payableService.registrarPago(100L, request, 1L));
    }

    @Test
    void registrarPago_WhenMontoExceedsSaldo_ShouldThrow() {
        // Given
        when(payableRepository.findById(100L)).thenReturn(Optional.of(payable));

        PayablePaymentRequest request = PayablePaymentRequest.builder()
                .monto(BigDecimal.valueOf(999999))
                .metodoPago("CASH")
                .build();

        // When / Then
        assertThrows(BadRequestException.class,
                () -> payableService.registrarPago(100L, request, 1L));
    }

    @Test
    void registrarPago_WhenInvalidMetodoPago_ShouldThrow() {
        // Given
        when(payableRepository.findById(100L)).thenReturn(Optional.of(payable));

        PayablePaymentRequest request = PayablePaymentRequest.builder()
                .monto(BigDecimal.valueOf(1000))
                .metodoPago("BITCOIN")
                .build();

        assertThrows(BadRequestException.class,
                () -> payableService.registrarPago(100L, request, 1L));
    }

    // ── actualizarEstados ──────────────────────────────────────────────

    @Test
    void actualizarEstados_WhenOverdue_ShouldMarkVencida() {
        // Given
        Payable vencida = Payable.builder()
                .id(200L).supplierId(1L)
                .montoOriginal(BigDecimal.valueOf(30000))
                .saldoPendiente(BigDecimal.valueOf(30000))
                .fechaEmision(today.minusDays(30))
                .fechaVencimiento(today.minusDays(5))
                .estado(Payable.Estado.PENDIENTE)
                .build();

        when(payableRepository.findVencidas(any())).thenReturn(List.of(vencida));

        // When
        payableService.actualizarEstados();

        // Then
        assertEquals(Payable.Estado.VENCIDA, vencida.getEstado());
        verify(payableRepository).save(vencida);
    }

    @Test
    void actualizarEstados_WhenNoneOverdue_ShouldDoNothing() {
        when(payableRepository.findVencidas(any())).thenReturn(List.of());

        payableService.actualizarEstados();

        verify(payableRepository, never()).save(any());
    }

    // ── totalPendienteEntreFechas ──────────────────────────────────────

    @Test
    void totalPendienteEntreFechas_ShouldReturnSum() {
        // Given
        when(payableRepository.totalPendienteEntreFechas(any(), any()))
                .thenReturn(BigDecimal.valueOf(150000));

        // When
        BigDecimal result = payableService.totalPendienteEntreFechas(today, today.plusDays(30));

        // Then
        assertEquals(BigDecimal.valueOf(150000), result);
    }
}
