package com.pos.system.service;

import com.pos.system.dto.request.ReceivablePaymentRequest;
import com.pos.system.dto.response.ReceivablePaymentResponse;
import com.pos.system.dto.response.ReceivableResponse;
import com.pos.system.entity.Client;
import com.pos.system.entity.PaymentMethod;
import com.pos.system.entity.Receivable;
import com.pos.system.entity.ReceivablePayment;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.ClientRepository;
import com.pos.system.repository.ReceivablePaymentRepository;
import com.pos.system.repository.ReceivableRepository;
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
class ReceivableServiceTest {

    @Mock private ReceivableRepository receivableRepository;
    @Mock private ReceivablePaymentRepository paymentRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private ConfigurationService configService;

    private ReceivableService receivableService;

    private Receivable receivable;
    private Client client;
    private final LocalDate today = LocalDate.of(2026, 6, 8);

    @BeforeEach
    void setUp() {
        receivableService = new ReceivableService(receivableRepository, paymentRepository,
                clientRepository, configService);

        client = Client.builder()
                .id(1L).name("Juan Pérez").documentNumber("30-12345678-9")
                .build();

        receivable = Receivable.builder()
                .id(100L)
                .clientId(1L)
                .saleId(10L)
                .montoOriginal(BigDecimal.valueOf(50000))
                .saldoPendiente(BigDecimal.valueOf(50000))
                .fechaEmision(today.minusDays(10))
                .fechaVencimiento(today.plusDays(20))
                .estado(Receivable.Estado.PENDIENTE)
                .interesesAcumulados(BigDecimal.ZERO)
                .build();
    }

    // ── createReceivable ────────────────────────────────────────────────

    @Test
    void createReceivable_ShouldCreateAndReturn() {
        // Given
        when(receivableRepository.save(any())).thenAnswer(invocation -> {
            Receivable saved = invocation.getArgument(0);
            saved.setId(200L);
            return saved;
        });

        // When
        Receivable result = receivableService.createReceivable(1L, 10L,
                BigDecimal.valueOf(30000), today.plusDays(30));

        // Then
        assertNotNull(result);
        assertEquals(200L, result.getId());
        assertEquals(BigDecimal.valueOf(30000), result.getMontoOriginal());
        assertEquals(BigDecimal.valueOf(30000), result.getSaldoPendiente());
        assertEquals(Receivable.Estado.PENDIENTE, result.getEstado());
        verify(receivableRepository).save(any());
    }

    // ── validarClienteParaCredito ────────────────────────────────────────

    @Test
    void validarClienteParaCredito_WhenNoDeuda_ShouldPass() {
        // Given
        when(receivableRepository.existsByClientIdAndEstadoIn(eq(1L), anyList()))
                .thenReturn(false);

        // When / Then
        assertDoesNotThrow(() -> receivableService.validarClienteParaCredito(1L));
    }

    @Test
    void validarClienteParaCredito_WhenHasDeudaVencida_ShouldThrow() {
        // Given
        when(receivableRepository.existsByClientIdAndEstadoIn(eq(1L), anyList()))
                .thenReturn(true);

        // When / Then
        assertThrows(BadRequestException.class,
                () -> receivableService.validarClienteParaCredito(1L));
    }

    // ── findByFilters ───────────────────────────────────────────────────

    @Test
    void findByFilters_WithoutFilters_ShouldReturnAll() {
        // Given
        when(receivableRepository.findByFilters(isNull(), isNull(), isNull()))
                .thenReturn(List.of(receivable));

        // When
        var page = receivableService.findByFilters(null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 20));

        // Then
        assertEquals(1, page.getTotalElements());
        assertEquals(100L, page.getContent().get(0).getId());
    }

    // ── getById ─────────────────────────────────────────────────────────

    @Test
    void getById_WhenExists_ShouldReturn() {
        // Given
        when(receivableRepository.findById(100L)).thenReturn(Optional.of(receivable));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        // When
        ReceivableResponse response = receivableService.getById(100L);

        // Then
        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Juan Pérez", response.getClientName());
    }

    @Test
    void getById_WhenNotFound_ShouldThrow() {
        when(receivableRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> receivableService.getById(999L));
    }

    // ── getPayments ─────────────────────────────────────────────────────

    @Test
    void getPayments_ShouldReturnList() {
        // Given
        ReceivablePayment payment = ReceivablePayment.builder()
                .id(1L).receivableId(100L)
                .monto(BigDecimal.valueOf(10000))
                .metodoPago(PaymentMethod.TRANSFER)
                .fecha(LocalDateTime.now()).registradoPor(1L)
                .build();
        when(paymentRepository.findByReceivableId(100L)).thenReturn(List.of(payment));

        // When
        List<ReceivablePaymentResponse> result = receivableService.getPayments(100L);

        // Then
        assertEquals(1, result.size());
        assertEquals(BigDecimal.valueOf(10000), result.get(0).getMonto());
    }

    // ── registrarPago ───────────────────────────────────────────────────

    @Test
    void registrarPago_WithFullPayment_ShouldMarkCobrada() {
        // Given
        when(receivableRepository.findById(100L)).thenReturn(Optional.of(receivable));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        ReceivablePaymentRequest request = ReceivablePaymentRequest.builder()
                .monto(BigDecimal.valueOf(50000))
                .metodoPago("TRANSFER")
                .build();

        // When
        ReceivableResponse result = receivableService.registrarPago(100L, request, 1L);

        // Then
        assertEquals("COBRADA", result.getEstado());
        assertEquals(BigDecimal.ZERO, result.getSaldoPendiente());
        verify(paymentRepository).save(any());
        verify(receivableRepository).save(any());
    }

    @Test
    void registrarPago_WithPartialPayment_ShouldMarkParcial() {
        // Given
        when(receivableRepository.findById(100L)).thenReturn(Optional.of(receivable));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        ReceivablePaymentRequest request = ReceivablePaymentRequest.builder()
                .monto(BigDecimal.valueOf(20000))
                .metodoPago("CASH")
                .build();

        // When
        ReceivableResponse result = receivableService.registrarPago(100L, request, 1L);

        // Then
        assertEquals("PARCIAL", result.getEstado());
        assertEquals(BigDecimal.valueOf(30000), result.getSaldoPendiente());
    }

    @Test
    void registrarPago_WhenAlreadyCobrada_ShouldThrow() {
        // Given
        receivable.setEstado(Receivable.Estado.COBRADA);
        when(receivableRepository.findById(100L)).thenReturn(Optional.of(receivable));

        ReceivablePaymentRequest request = ReceivablePaymentRequest.builder()
                .monto(BigDecimal.valueOf(1000))
                .metodoPago("CASH")
                .build();

        // When / Then
        assertThrows(BadRequestException.class,
                () -> receivableService.registrarPago(100L, request, 1L));
    }

    @Test
    void registrarPago_WhenInCobrable_ShouldThrow() {
        // Given
        receivable.setEstado(Receivable.Estado.INCOBRABLE);
        when(receivableRepository.findById(100L)).thenReturn(Optional.of(receivable));

        ReceivablePaymentRequest request = ReceivablePaymentRequest.builder()
                .monto(BigDecimal.valueOf(1000))
                .metodoPago("CASH")
                .build();

        // When / Then
        assertThrows(BadRequestException.class,
                () -> receivableService.registrarPago(100L, request, 1L));
    }

    @Test
    void registrarPago_WhenMontoExceedsSaldo_ShouldThrow() {
        // Given
        when(receivableRepository.findById(100L)).thenReturn(Optional.of(receivable));

        ReceivablePaymentRequest request = ReceivablePaymentRequest.builder()
                .monto(BigDecimal.valueOf(99999))
                .metodoPago("CASH")
                .build();

        // When / Then
        assertThrows(BadRequestException.class,
                () -> receivableService.registrarPago(100L, request, 1L));
    }

    @Test
    void registrarPago_WhenInvalidMetodoPago_ShouldThrow() {
        // Given
        when(receivableRepository.findById(100L)).thenReturn(Optional.of(receivable));

        ReceivablePaymentRequest request = ReceivablePaymentRequest.builder()
                .monto(BigDecimal.valueOf(1000))
                .metodoPago("INVALID")
                .build();

        // When / Then
        assertThrows(BadRequestException.class,
                () -> receivableService.registrarPago(100L, request, 1L));
    }

    // ── actualizarEstados ──────────────────────────────────────────────

    @Test
    void actualizarEstados_WhenOverdue_ShouldMarkVencida() {
        // Given
        Receivable vencida = Receivable.builder()
                .id(200L).clientId(1L).saleId(20L)
                .montoOriginal(BigDecimal.valueOf(10000))
                .saldoPendiente(BigDecimal.valueOf(10000))
                .fechaEmision(today.minusDays(40))
                .fechaVencimiento(today.minusDays(10))
                .estado(Receivable.Estado.PENDIENTE)
                .build();

        when(receivableRepository.findVencidas(any())).thenReturn(List.of(vencida));

        // When
        receivableService.actualizarEstados();

        // Then
        assertEquals(Receivable.Estado.VENCIDA, vencida.getEstado());
        verify(receivableRepository).save(vencida);
    }

    @Test
    void actualizarEstados_WhenNoneOverdue_ShouldDoNothing() {
        // Given
        when(receivableRepository.findVencidas(any())).thenReturn(List.of());

        // When
        receivableService.actualizarEstados();

        // Then
        verify(receivableRepository, never()).save(any());
    }

    // ── calcularIntereses ───────────────────────────────────────────────

    @Test
    void calcularIntereses_WhenTasaConfigurada_ShouldCalculate() {
        // Given
        Receivable vencida = Receivable.builder()
                .id(300L).clientId(1L).saleId(30L)
                .montoOriginal(BigDecimal.valueOf(100000))
                .saldoPendiente(BigDecimal.valueOf(100000))
                .fechaEmision(today.minusDays(60))
                .fechaVencimiento(today.minusDays(30))
                .estado(Receivable.Estado.VENCIDA)
                .interesesAcumulados(BigDecimal.ZERO)
                .build();

        when(receivableRepository.findByEstado(Receivable.Estado.VENCIDA))
                .thenReturn(List.of(vencida));
        when(configService.getDoubleConfig("mora.tasa")).thenReturn(0.50); // 50% anual

        // When
        receivableService.calcularIntereses();

        // Then
        // 30 days * (0.50 / 365) * 100000 = ~4109.59
        assertTrue(vencida.getInteresesAcumulados().compareTo(BigDecimal.ZERO) > 0);
        verify(receivableRepository).save(vencida);
    }

    @Test
    void calcularIntereses_WhenNoTasa_ShouldUseZero() {
        // Given
        Receivable vencida = Receivable.builder()
                .id(400L).clientId(1L).saleId(40L)
                .montoOriginal(BigDecimal.valueOf(50000))
                .saldoPendiente(BigDecimal.valueOf(50000))
                .fechaEmision(today.minusDays(30))
                .fechaVencimiento(today.minusDays(5))
                .estado(Receivable.Estado.VENCIDA)
                .interesesAcumulados(BigDecimal.ZERO)
                .build();

        when(receivableRepository.findByEstado(Receivable.Estado.VENCIDA))
                .thenReturn(List.of(vencida));
        when(configService.getDoubleConfig("mora.tasa")).thenThrow(new RuntimeException());

        // When
        receivableService.calcularIntereses();

        // Then — no intereses should be added (scale 2 from setScale in service)
        assertEquals(0, BigDecimal.ZERO.compareTo(vencida.getInteresesAcumulados()));
        verify(receivableRepository).save(vencida);
    }
}
