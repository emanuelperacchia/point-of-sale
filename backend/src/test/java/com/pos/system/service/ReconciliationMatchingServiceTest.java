package com.pos.system.service;

import com.pos.system.dto.request.CreateExpenseFromStatementRequest;
import com.pos.system.entity.*;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class ReconciliationMatchingServiceTest {

    @Mock private BankReconciliationRepository reconciliationRepository;
    @Mock private BankStatementRepository statementRepository;
    @Mock private ReceivablePaymentRepository receivablePaymentRepository;
    @Mock private PayablePaymentRepository payablePaymentRepository;
    @Mock private ExpenseService expenseService;

    private ReconciliationMatchingService matchingService;

    private BankReconciliation reconciliation;
    private BankStatement statement;
    private final LocalDate today = LocalDate.of(2026, 6, 8);

    @BeforeEach
    void setUp() {
        matchingService = new ReconciliationMatchingService(
                reconciliationRepository, statementRepository,
                receivablePaymentRepository, payablePaymentRepository,
                expenseService);

        reconciliation = BankReconciliation.builder()
                .id(1L).periodo("2026-06")
                .totalExtracto(BigDecimal.ZERO)
                .totalSistema(BigDecimal.ZERO)
                .diferencia(BigDecimal.ZERO)
                .estado(BankReconciliation.Estado.ABIERTA)
                .build();

        statement = BankStatement.builder()
                .id(10L).reconciliationId(1L)
                .fecha(today).descripcion("Deposito")
                .monto(BigDecimal.valueOf(50000))
                .tipo(BankStatement.TipoMovimiento.CREDITO)
                .estado(BankStatement.EstadoConciliacion.PENDIENTE)
                .build();
    }

    // ── autoMatch ───────────────────────────────────────────────────────

    @Test
    void autoMatch_WhenSingleReceivableMatch_ShouldConciliate() {
        // Given
        when(reconciliationRepository.findById(1L)).thenReturn(Optional.of(reconciliation));
        when(statementRepository.findByReconciliationIdAndEstado(1L,
                BankStatement.EstadoConciliacion.PENDIENTE))
                .thenReturn(List.of(statement));

        ReceivablePayment receivablePayment = ReceivablePayment.builder()
                .id(99L).receivableId(100L)
                .monto(BigDecimal.valueOf(50000))
                .metodoPago(PaymentMethod.TRANSFER)
                .fecha(today.atStartOfDay().plusHours(2))
                .registradoPor(1L)
                .build();

        when(receivablePaymentRepository.findByFechaBetween(any(), any()))
                .thenReturn(List.of(receivablePayment));
        when(payablePaymentRepository.findByFechaBetween(any(), any()))
                .thenReturn(List.of());

        when(statementRepository.findByReconciliationId(1L)).thenReturn(List.of(statement));

        // When
        int matched = matchingService.autoMatch(1L);

        // Then
        assertEquals(1, matched);
        assertEquals(BankStatement.EstadoConciliacion.CONCILIADO, statement.getEstado());
        assertEquals(99L, statement.getPaymentId());
        verify(statementRepository).save(statement);
    }

    @Test
    void autoMatch_WhenMultipleMatches_ShouldNotConciliate() {
        // Given
        when(reconciliationRepository.findById(1L)).thenReturn(Optional.of(reconciliation));
        when(statementRepository.findByReconciliationIdAndEstado(1L,
                BankStatement.EstadoConciliacion.PENDIENTE))
                .thenReturn(List.of(statement));

        // Two receivable payments with same amount in window
        ReceivablePayment rp1 = ReceivablePayment.builder()
                .id(1L).receivableId(100L)
                .monto(BigDecimal.valueOf(50000))
                .metodoPago(PaymentMethod.CASH)
                .fecha(today.atStartOfDay().plusHours(1))
                .registradoPor(1L)
                .build();

        ReceivablePayment rp2 = ReceivablePayment.builder()
                .id(2L).receivableId(101L)
                .monto(BigDecimal.valueOf(50000))
                .metodoPago(PaymentMethod.TRANSFER)
                .fecha(today.atStartOfDay().plusHours(3))
                .registradoPor(1L)
                .build();

        when(receivablePaymentRepository.findByFechaBetween(any(), any()))
                .thenReturn(List.of(rp1, rp2));
        when(payablePaymentRepository.findByFechaBetween(any(), any()))
                .thenReturn(List.of());

        // When
        int matched = matchingService.autoMatch(1L);

        // Then — should stay PENDIENTE due to ambiguity
        assertEquals(0, matched);
        assertEquals(BankStatement.EstadoConciliacion.PENDIENTE, statement.getEstado());
    }

    @Test
    void autoMatch_WhenPayableMatch_ShouldConciliate() {
        // Given
        when(reconciliationRepository.findById(1L)).thenReturn(Optional.of(reconciliation));
        when(statementRepository.findByReconciliationIdAndEstado(1L,
                BankStatement.EstadoConciliacion.PENDIENTE))
                .thenReturn(List.of(statement));

        PayablePayment payablePayment = PayablePayment.builder()
                .id(77L).payableId(200L)
                .monto(BigDecimal.valueOf(50000))
                .metodoPago(PaymentMethod.TRANSFER)
                .fecha(today.atStartOfDay().plusHours(2))
                .registradoPor(1L)
                .build();

        when(receivablePaymentRepository.findByFechaBetween(any(), any()))
                .thenReturn(List.of());
        when(payablePaymentRepository.findByFechaBetween(any(), any()))
                .thenReturn(List.of(payablePayment));
        when(statementRepository.findByReconciliationId(1L)).thenReturn(List.of(statement));

        // When
        int matched = matchingService.autoMatch(1L);

        // Then
        assertEquals(1, matched);
        assertEquals(77L, statement.getPaymentId());
    }

    @Test
    void autoMatch_WhenReconciliationIsCerrada_ShouldThrow() {
        // Given
        reconciliation.setEstado(BankReconciliation.Estado.CERRADA);
        when(reconciliationRepository.findById(1L)).thenReturn(Optional.of(reconciliation));

        // When / Then
        assertThrows(BadRequestException.class, () -> matchingService.autoMatch(1L));
    }

    // ── manualMatch ─────────────────────────────────────────────────────

    @Test
    void manualMatch_WithReceivablePayment_ShouldConciliate() {
        // Given
        when(statementRepository.findById(10L)).thenReturn(Optional.of(statement));
        when(receivablePaymentRepository.findById(99L)).thenReturn(
                Optional.of(ReceivablePayment.builder().id(99L).build()));
        when(reconciliationRepository.findById(1L)).thenReturn(Optional.of(reconciliation));
        when(statementRepository.findByReconciliationId(1L)).thenReturn(List.of(statement));

        // When
        BankStatement result = matchingService.manualMatch(10L, 99L, "RECEIVABLE_PAYMENT");

        // Then
        assertEquals(BankStatement.EstadoConciliacion.CONCILIADO, result.getEstado());
        assertEquals(99L, result.getPaymentId());
        assertTrue(result.getObservacion().contains("manual"));
    }

    @Test
    void manualMatch_WithPayablePayment_ShouldConciliate() {
        // Given
        when(statementRepository.findById(10L)).thenReturn(Optional.of(statement));
        when(payablePaymentRepository.findById(77L)).thenReturn(
                Optional.of(PayablePayment.builder().id(77L).build()));
        when(reconciliationRepository.findById(1L)).thenReturn(Optional.of(reconciliation));
        when(statementRepository.findByReconciliationId(1L)).thenReturn(List.of(statement));

        // When
        BankStatement result = matchingService.manualMatch(10L, 77L, "PAYABLE_PAYMENT");

        // Then
        assertEquals(BankStatement.EstadoConciliacion.CONCILIADO, result.getEstado());
    }

    @Test
    void manualMatch_WhenInvalidTipo_ShouldThrow() {
        when(statementRepository.findById(10L)).thenReturn(Optional.of(statement));

        assertThrows(BadRequestException.class,
                () -> matchingService.manualMatch(10L, 99L, "INVALID_TYPE"));
    }

    // ── createExpenseFromStatement ──────────────────────────────────────

    @Test
    void createExpenseFromStatement_OnDebito_ShouldCreateExpense() {
        // Given
        statement.setTipo(BankStatement.TipoMovimiento.DEBITO);

        when(statementRepository.findById(10L)).thenReturn(Optional.of(statement));
        when(reconciliationRepository.findById(1L)).thenReturn(Optional.of(reconciliation));
        when(statementRepository.findByReconciliationId(1L)).thenReturn(List.of(statement));

        CreateExpenseFromStatementRequest request = CreateExpenseFromStatementRequest.builder()
                .statementId(10L)
                .monto(BigDecimal.valueOf(2500))
                .categoria("SERVICIOS")
                .descripcion("Comision bancaria")
                .build();

        // When
        BankStatement result = matchingService.createExpenseFromStatement(request, 1L);

        // Then
        assertEquals(BankStatement.EstadoConciliacion.AJUSTE_MANUAL, result.getEstado());
        verify(expenseService).create(any(Expense.class), isNull());
    }

    @Test
    void createExpenseFromStatement_WhenCredito_ShouldThrow() {
        // Given
        statement.setTipo(BankStatement.TipoMovimiento.CREDITO);
        when(statementRepository.findById(10L)).thenReturn(Optional.of(statement));

        CreateExpenseFromStatementRequest request = CreateExpenseFromStatementRequest.builder()
                .statementId(10L)
                .monto(BigDecimal.valueOf(1000))
                .categoria("OTROS")
                .descripcion("Test")
                .build();

        assertThrows(BadRequestException.class,
                () -> matchingService.createExpenseFromStatement(request, 1L));
    }

    @Test
    void createExpenseFromStatement_WhenAlreadyMatched_ShouldThrow() {
        // Given
        statement.setEstado(BankStatement.EstadoConciliacion.CONCILIADO);
        when(statementRepository.findById(10L)).thenReturn(Optional.of(statement));

        CreateExpenseFromStatementRequest request = CreateExpenseFromStatementRequest.builder()
                .statementId(10L)
                .monto(BigDecimal.valueOf(1000))
                .categoria("OTROS")
                .descripcion("Test")
                .build();

        assertThrows(BadRequestException.class,
                () -> matchingService.createExpenseFromStatement(request, 1L));
    }

    @Test
    void createExpenseFromStatement_WhenNotFound_ShouldThrow() {
        when(statementRepository.findById(999L)).thenReturn(Optional.empty());

        CreateExpenseFromStatementRequest request = CreateExpenseFromStatementRequest.builder()
                .statementId(999L)
                .monto(BigDecimal.valueOf(1000))
                .categoria("OTROS")
                .descripcion("Test")
                .build();

        assertThrows(ResourceNotFoundException.class,
                () -> matchingService.createExpenseFromStatement(request, 1L));
    }
}
