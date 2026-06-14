package com.pos.system.service;

import com.pos.system.dto.response.ExpenseResponse;
import com.pos.system.dto.response.ExpenseSummaryResponse;
import com.pos.system.entity.Expense;
import com.pos.system.entity.Expense.ExpenseCategory;
import com.pos.system.entity.Expense.ExpenseEstado;
import com.pos.system.entity.Expense.ExpenseFrecuencia;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock private ExpenseRepository expenseRepository;
    @Mock private FileStorageService fileStorageService;

    private ExpenseService expenseService;

    private Expense expense;
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        expenseService = new ExpenseService(expenseRepository, fileStorageService);

        expense = Expense.builder()
                .id(1L)
                .monto(BigDecimal.valueOf(15000))
                .fecha(today)
                .categoria(ExpenseCategory.SERVICIOS)
                .proveedorId(1L)
                .descripcion("Pago de luz")
                .estado(ExpenseEstado.PENDIENTE)
                .recurrente(false)
                .build();
    }

    // ── Create ──────────────────────────────────────────────────────────

    @Test
    void create_WithoutComprobante_ShouldSaveExpense() {
        // Given
        Expense newExpense = Expense.builder()
                .monto(BigDecimal.valueOf(10000))
                .fecha(today)
                .categoria(ExpenseCategory.ALQUILER)
                .descripcion("Alquiler local")
                .build();

        when(expenseRepository.save(any())).thenAnswer(invocation -> {
            Expense saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        // When
        ExpenseResponse result = expenseService.create(newExpense, null);

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(10000), result.monto());
        assertEquals("ALQUILER", result.categoria());
        assertEquals("PENDIENTE", result.estado());
        assertNull(result.comprobanteUrl());
        verify(expenseRepository).save(any());
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void create_WithComprobante_ShouldSaveAndUploadFile() {
        // Given
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(fileStorageService.save(file, "expenses")).thenReturn("/uploads/expenses/uuid.pdf");

        Expense newExpense = Expense.builder()
                .monto(BigDecimal.valueOf(5000))
                .fecha(today)
                .categoria(ExpenseCategory.OTROS)
                .descripcion("Compra insumos")
                .build();

        when(expenseRepository.save(any())).thenAnswer(invocation -> {
            Expense saved = invocation.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        // When
        ExpenseResponse result = expenseService.create(newExpense, file);

        // Then
        assertNotNull(result);
        assertEquals("/uploads/expenses/uuid.pdf", result.comprobanteUrl());
        assertTrue(result.tieneComprobante());
        verify(fileStorageService).save(file, "expenses");
        verify(expenseRepository).save(any());
    }

    @Test
    void create_WithRecurrente_ShouldSetProximaFecha() {
        // Given
        Expense recurrente = Expense.builder()
                .monto(BigDecimal.valueOf(30000))
                .fecha(today)
                .categoria(ExpenseCategory.ALQUILER)
                .descripcion("Alquiler")
                .recurrente(true)
                .frecuencia(ExpenseFrecuencia.MENSUAL)
                .build();

        when(expenseRepository.save(any())).thenAnswer(invocation -> {
            Expense saved = invocation.getArgument(0);
            saved.setId(4L);
            return saved;
        });

        // When
        ExpenseResponse result = expenseService.create(recurrente, null);

        // Then
        assertNotNull(result);
        assertTrue(result.recurrente());
        assertEquals("MENSUAL", result.frecuencia());
        assertEquals(today.plusMonths(1), result.proximaFecha());
    }

    // ── GetById ─────────────────────────────────────────────────────────

    @Test
    void getById_WhenExists_ShouldReturnExpense() {
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));

        ExpenseResponse result = expenseService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Pago de luz", result.descripcion());
    }

    @Test
    void getById_WhenNotExists_ShouldThrow() {
        when(expenseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> expenseService.getById(99L));
    }

    // ── GetAll ──────────────────────────────────────────────────────────

    @Test
    void getAll_WithFilters_ShouldReturnFilteredList() {
        when(expenseRepository.findByFilters(
                ExpenseCategory.SERVICIOS, ExpenseEstado.PENDIENTE,
                today, today, 1L))
                .thenReturn(List.of(expense));

        List<ExpenseResponse> results = expenseService.getAll(
                "SERVICIOS", "PENDIENTE", today, today, 1L);

        assertEquals(1, results.size());
        assertEquals("SERVICIOS", results.get(0).categoria());
    }

    @Test
    void getAll_WithNullFilters_ShouldReturnAll() {
        when(expenseRepository.findByFilters(
                null, null, null, null, null))
                .thenReturn(List.of(expense));

        List<ExpenseResponse> results = expenseService.getAll(null, null, null, null, null);

        assertEquals(1, results.size());
    }

    // ── Update ──────────────────────────────────────────────────────────

    @Test
    void update_WhenExists_ShouldUpdateFields() {
        // Given
        Expense existing = Expense.builder()
                .id(1L).monto(BigDecimal.valueOf(10000))
                .fecha(today).categoria(ExpenseCategory.SERVICIOS)
                .descripcion("Vieja descripcion").estado(ExpenseEstado.PENDIENTE)
                .recurrente(false)
                .build();

        Expense updateData = Expense.builder()
                .monto(BigDecimal.valueOf(12000))
                .descripcion("Nueva descripcion")
                .build();

        when(expenseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(expenseRepository.save(any())).thenReturn(existing);

        // When
        ExpenseResponse result = expenseService.update(1L, updateData, null);

        // Then
        assertEquals(0, BigDecimal.valueOf(12000).compareTo(result.monto()));
        assertEquals("Nueva descripcion", result.descripcion());
        verify(expenseRepository).save(existing);
    }

    @Test
    void update_WhenNotExists_ShouldThrow() {
        when(expenseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> expenseService.update(99L, expense, null));
    }

    @Test
    void update_WithNewComprobante_ShouldDeleteOldAndSaveNew() {
        // Given
        Expense existing = Expense.builder()
                .id(1L).monto(BigDecimal.valueOf(10000))
                .fecha(today).categoria(ExpenseCategory.SERVICIOS)
                .descripcion("Test").estado(ExpenseEstado.PENDIENTE)
                .comprobanteUrl("/uploads/expenses/old.pdf")
                .recurrente(false)
                .build();

        MultipartFile newFile = mock(MultipartFile.class);
        when(newFile.isEmpty()).thenReturn(false);
        when(fileStorageService.save(newFile, "expenses")).thenReturn("/uploads/expenses/new.pdf");

        when(expenseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(expenseRepository.save(any())).thenReturn(existing);

        // When
        ExpenseResponse result = expenseService.update(1L, expense, newFile);

        // Then
        assertEquals("/uploads/expenses/new.pdf", result.comprobanteUrl());
        verify(fileStorageService).delete("/uploads/expenses/old.pdf");
        verify(fileStorageService).save(newFile, "expenses");
    }

    @Test
    void update_WithRecurrenteTrue_ShouldSetProximaFecha() {
        // Given
        Expense existing = Expense.builder()
                .id(1L).monto(BigDecimal.valueOf(10000))
                .fecha(today).categoria(ExpenseCategory.SERVICIOS)
                .descripcion("Test").estado(ExpenseEstado.PENDIENTE)
                .recurrente(false)
                .build();

        Expense updateData = Expense.builder()
                .recurrente(true)
                .frecuencia(ExpenseFrecuencia.MENSUAL)
                .build();

        when(expenseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(expenseRepository.save(any())).thenReturn(existing);

        // When
        ExpenseResponse result = expenseService.update(1L, updateData, null);

        // Then
        assertTrue(result.recurrente());
        assertEquals("MENSUAL", result.frecuencia());
        assertEquals(today.plusMonths(1), result.proximaFecha());
    }

    // ── Delete ──────────────────────────────────────────────────────────

    @Test
    void delete_WhenExists_ShouldDelete() {
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));

        expenseService.delete(1L);

        verify(expenseRepository).deleteById(1L);
    }

    @Test
    void delete_WithComprobante_ShouldDeleteFileToo() {
        expense.setComprobanteUrl("/uploads/expenses/doc.pdf");
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));

        expenseService.delete(1L);

        verify(fileStorageService).delete("/uploads/expenses/doc.pdf");
        verify(expenseRepository).deleteById(1L);
    }

    @Test
    void delete_WhenNotExists_ShouldThrow() {
        when(expenseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> expenseService.delete(99L));
    }

    // ── Marcar Pagado ───────────────────────────────────────────────────

    @Test
    void marcarPagado_WhenExists_ShouldSetEstadoPagado() {
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any())).thenReturn(expense);

        expenseService.marcarPagado(1L);

        assertEquals(ExpenseEstado.PAGADO, expense.getEstado());
        verify(expenseRepository).save(expense);
    }

    @Test
    void marcarPagado_WhenNotExists_ShouldThrow() {
        when(expenseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> expenseService.marcarPagado(99L));
    }

    // ── Summary ─────────────────────────────────────────────────────────

    @Test
    void getSummary_ShouldReturnTotalsByCategoria() {
        // Given
        Object[] row1 = new Object[]{"SERVICIOS", BigDecimal.valueOf(50000)};
        Object[] row2 = new Object[]{"ALQUILER", BigDecimal.valueOf(100000)};

        when(expenseRepository.findTotalsByCategoria(today, today.plusDays(30)))
                .thenReturn(List.<Object[]>of(row1, row2));

        // When
        ExpenseSummaryResponse summary = expenseService.getSummary(today, today.plusDays(30));

        // Then
        assertEquals(2, summary.categorias().size());
        assertEquals("SERVICIOS", summary.categorias().get(0).categoria());
        assertEquals(0, BigDecimal.valueOf(50000).compareTo(summary.categorias().get(0).total()));
        assertEquals(0, BigDecimal.valueOf(150000).compareTo(summary.total()));
    }

    @Test
    void getSummary_WhenNoData_ShouldReturnZeroTotal() {
        when(expenseRepository.findTotalsByCategoria(any(), any()))
                .thenReturn(List.of());

        ExpenseSummaryResponse summary = expenseService.getSummary(today, today.plusDays(30));

        assertTrue(summary.categorias().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.total()));
    }

    // ── Generar Recurrentes ─────────────────────────────────────────────

    @Test
    void generarRecurrentes_WhenHayVencidos_ShouldCreateCopiesAndAdvanceDates() {
        // Given
        Expense original = Expense.builder()
                .id(1L)
                .monto(BigDecimal.valueOf(30000))
                .fecha(today.minusMonths(1))
                .categoria(ExpenseCategory.ALQUILER)
                .proveedorId(1L)
                .descripcion("Alquiler local")
                .estado(ExpenseEstado.PENDIENTE)
                .recurrente(true)
                .frecuencia(ExpenseFrecuencia.MENSUAL)
                .proximaFecha(today)
                .build();

        when(expenseRepository.findRecurrentesVencidos(today))
                .thenReturn(List.of(original));

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);

        // When
        expenseService.generarRecurrentes();

        // Then
        verify(expenseRepository, times(2)).save(captor.capture());
        List<Expense> saved = captor.getAllValues();

        // First save: la nueva copia
        Expense nuevo = saved.get(0);
        assertEquals(0, BigDecimal.valueOf(30000).compareTo(nuevo.getMonto()));
        assertEquals(today, nuevo.getFecha());
        assertEquals(ExpenseEstado.PENDIENTE, nuevo.getEstado());
        assertFalse(nuevo.getRecurrente()); // la copia no es recurrente
        assertTrue(nuevo.getDescripcion().contains("(recurrente)"));

        // Second save: el original con fecha avanzada
        Expense actualizado = saved.get(1);
        assertEquals(today.plusMonths(1), actualizado.getProximaFecha());
    }

    @Test
    void generarRecurrentes_WhenNoVencidos_ShouldDoNothing() {
        when(expenseRepository.findRecurrentesVencidos(today))
                .thenReturn(List.of());

        expenseService.generarRecurrentes();

        verify(expenseRepository, never()).save(any());
    }
}
