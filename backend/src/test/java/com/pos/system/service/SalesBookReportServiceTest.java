package com.pos.system.service;

import com.pos.system.dto.response.SalesBookResponse;
import com.pos.system.dto.response.SalesBookRow;
import com.pos.system.entity.InvoiceStatus;
import com.pos.system.entity.TipoComprobante;
import com.pos.system.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesBookReportServiceTest {

    @Mock private InvoiceRepository invoiceRepository;

    private SalesBookReportService salesBookReportService;

    private final LocalDate desde = LocalDate.of(2026, 1, 1);
    private final LocalDate hasta = LocalDate.of(2026, 1, 31);

    @BeforeEach
    void setUp() {
        salesBookReportService = new SalesBookReportService(invoiceRepository);
    }

    @Test
    void getSalesBook_WhenHasData_ShouldReturnRowsAndTotals() {
        // Given
        Object[] row1 = buildRawRow(
                LocalDateTime.of(2026, 1, 5, 10, 0),
                "FACTURA_B", 1, 100L, "20-12345678-9", "Cliente S.A.",
                BigDecimal.valueOf(1000), BigDecimal.ZERO,
                BigDecimal.valueOf(210), BigDecimal.valueOf(1210), "EMITIDO"
        );
        Object[] row2 = buildRawRow(
                LocalDateTime.of(2026, 1, 10, 14, 30),
                "FACTURA_B", 1, 101L, "20-87654321-0", "Otro Cliente",
                BigDecimal.valueOf(2000), BigDecimal.valueOf(100),
                BigDecimal.valueOf(399), BigDecimal.valueOf(2299), "EMITIDO"
        );

        when(invoiceRepository.findSalesBookRaw(
                eq(null), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.<Object[]>of(row1, row2));

        // When
        SalesBookResponse response = salesBookReportService.getSalesBook(desde, hasta, null);

        // Then
        assertNotNull(response);
        assertEquals(2, response.filas().size());
        assertEquals(2, response.totales().cantidad());
        assertEquals(0, BigDecimal.valueOf(2900).compareTo(response.totales().totalNeto())); // (1000-0) + (2000-100)
        assertEquals(0, BigDecimal.valueOf(609).compareTo(response.totales().totalIva()));
        assertEquals(0, BigDecimal.valueOf(3509).compareTo(response.totales().totalComprobantes()));
        assertFalse(response.haySaltos());
    }

    @Test
    void getSalesBook_WhenHasAnulado_ShouldSetImportesCero() {
        // Given
        Object[] row = buildRawRow(
                LocalDateTime.of(2026, 1, 5, 10, 0),
                "FACTURA_A", 1, 50L, "20-12345678-9", "Cliente S.A.",
                BigDecimal.valueOf(500), BigDecimal.ZERO,
                BigDecimal.valueOf(105), BigDecimal.valueOf(605), "ANULADO"
        );

        when(invoiceRepository.findSalesBookRaw(
                any(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.<Object[]>of(row));

        // When
        SalesBookResponse response = salesBookReportService.getSalesBook(desde, hasta, "FACTURA_A");

        // Then
        SalesBookRow result = response.filas().get(0);
        assertEquals(InvoiceStatus.ANULADO, result.estado());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.netoGravado()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.iva()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.total()));
    }

    @Test
    void getSalesBook_WhenNoData_ShouldReturnEmpty() {
        // Given
        when(invoiceRepository.findSalesBookRaw(
                any(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // When
        SalesBookResponse response = salesBookReportService.getSalesBook(desde, hasta, null);

        // Then
        assertNotNull(response);
        assertTrue(response.filas().isEmpty());
        assertEquals(0, response.totales().cantidad());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.totales().totalNeto()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.totales().totalIva()));
        assertFalse(response.haySaltos());
    }

    @Test
    void detectGaps_WhenConsecutiveNumbers_ShouldReturnEmpty() {
        // Given
        List<Object[]> raw = List.of(
                buildMinimalRow("FACTURA_B", 1L),
                buildMinimalRow("FACTURA_B", 2L),
                buildMinimalRow("FACTURA_B", 3L)
        );

        // When
        List<Long> gaps = salesBookReportService.detectGaps(raw);

        // Then
        assertTrue(gaps.isEmpty());
    }

    @Test
    void detectGaps_WhenNonConsecutiveNumbers_ShouldReturnGaps() {
        // Given
        List<Object[]> raw = List.of(
                buildMinimalRow("FACTURA_B", 1L),
                buildMinimalRow("FACTURA_B", 5L)
        );

        // When
        List<Long> gaps = salesBookReportService.detectGaps(raw);

        // Then
        assertEquals(3, gaps.size());
        assertTrue(gaps.contains(2L));
        assertTrue(gaps.contains(3L));
        assertTrue(gaps.contains(4L));
    }

    @Test
    void detectGaps_WhenMultipleTipos_ShouldDetectGapsPerTipo() {
        // Given
        List<Object[]> raw = List.of(
                buildMinimalRow("FACTURA_A", 10L),
                buildMinimalRow("FACTURA_A", 12L),
                buildMinimalRow("FACTURA_B", 1L),
                buildMinimalRow("FACTURA_B", 4L)
        );

        // When
        List<Long> gaps = salesBookReportService.detectGaps(raw);

        // Then
        assertEquals(3, gaps.size());
        // FACTURA_A gap: 11
        assertTrue(gaps.contains(11L));
        // FACTURA_B gaps: 2, 3
        assertTrue(gaps.contains(2L));
        assertTrue(gaps.contains(3L));
    }

    @Test
    void detectGaps_WhenNullRaw_ShouldReturnEmpty() {
        // When
        List<Long> gaps = salesBookReportService.detectGaps(null);

        // Then
        assertNotNull(gaps);
        assertTrue(gaps.isEmpty());
    }

    @Test
    void detectGaps_WhenEmptyRaw_ShouldReturnEmpty() {
        // When
        List<Long> gaps = salesBookReportService.detectGaps(List.of());

        // Then
        assertTrue(gaps.isEmpty());
    }

    @Test
    void getSalesBook_WithDiscount_ShouldCalculateCorrectNeto() {
        // Given
        Object[] row = buildRawRow(
                LocalDateTime.of(2026, 1, 15, 12, 0),
                "FACTURA_B", 1, 200L, "20-11111111-1", "Cliente",
                BigDecimal.valueOf(1000), BigDecimal.valueOf(200),
                BigDecimal.valueOf(168), BigDecimal.valueOf(968), "EMITIDO"
        );

        when(invoiceRepository.findSalesBookRaw(
                any(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.<Object[]>of(row));

        // When
        SalesBookResponse response = salesBookReportService.getSalesBook(desde, hasta, null);

        // Then
        SalesBookRow result = response.filas().get(0);
        assertEquals(0, BigDecimal.valueOf(800).compareTo(result.netoGravado())); // 1000 - 200
    }

    @Test
    void getRowsForExport_ShouldReturnRowsWithoutPagination() {
        // Given
        Object[] row = buildRawRow(
                LocalDateTime.of(2026, 1, 5, 10, 0),
                "FACTURA_B", 1, 1L, "20-12345678-9", "Cliente",
                BigDecimal.valueOf(500), BigDecimal.ZERO,
                BigDecimal.valueOf(105), BigDecimal.valueOf(605), "EMITIDO"
        );

        when(invoiceRepository.findSalesBookRaw(
                any(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.<Object[]>of(row));

        // When
        List<SalesBookRow> rows = salesBookReportService.getRowsForExport(desde, hasta, null);

        // Then
        assertEquals(1, rows.size());
        assertEquals("Cliente", rows.get(0).razonSocial());
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private Object[] buildRawRow(LocalDateTime fecha, String tipo, Integer ptoVta,
                                  Long numero, String cuit, String razonSocial,
                                  BigDecimal subtotal, BigDecimal discount,
                                  BigDecimal iva, BigDecimal total, String estado) {
        return new Object[]{fecha, tipo, ptoVta, numero, cuit, razonSocial,
                subtotal, discount, iva, total, estado};
    }

    private Object[] buildMinimalRow(String tipo, Long numero) {
        return new Object[]{null, tipo, null, numero, null, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "EMITIDO"};
    }
}
