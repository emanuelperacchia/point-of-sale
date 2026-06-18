package com.pos.system.service;

import com.pos.system.dto.response.SalesReportResponse;
import com.pos.system.entity.SaleStatus;
import com.pos.system.repository.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesReportServiceTest {

    @Mock private SaleRepository saleRepository;

    private SalesReportService salesReportService;

    private final LocalDate desde = LocalDate.of(2026, 6, 1);
    private final LocalDate hasta = LocalDate.of(2026, 6, 15);

    @BeforeEach
    void setUp() {
        salesReportService = new SalesReportService(saleRepository);
    }

    @Test
    void advancedReport_ShouldReturnCompleteReport() {
        // Given
        stubBasicMetrics();
        stubSalesByPaymentMethod();
        stubSalesByHour();
        stubSalesByDayOfWeek();
        stubDailySales();

        // When
        SalesReportResponse res = salesReportService.advancedReport(desde, hasta);

        // Then
        assertNotNull(res);
        assertNotNull(res.getResumen());
        assertEquals("OK", res.getResumen().getStatus());

        // Metrics
        assertEquals(0, BigDecimal.valueOf(15000).compareTo(res.getResumen().getTotalVentas()));
        assertEquals(30L, res.getResumen().getCantidadTransacciones());
        assertEquals(0, BigDecimal.valueOf(500).compareTo(res.getResumen().getTicketPromedio()));
        assertEquals(0, BigDecimal.valueOf(500).compareTo(res.getResumen().getTotalDevoluciones()));
        assertEquals(0, BigDecimal.valueOf(200).compareTo(res.getResumen().getDescuentosAplicados()));
        assertEquals(0, BigDecimal.valueOf(3000).compareTo(res.getResumen().getImpuestosCobrados()));

        // Payment methods
        assertFalse(res.getPorMetodoPago().isEmpty());
        assertEquals(2, res.getPorMetodoPago().size());
        assertEquals("CASH", res.getPorMetodoPago().get(0).getMetodo());

        // Hourly
        assertFalse(res.getVentasPorHora().isEmpty());

        // Day of week
        assertFalse(res.getVentasPorDiaSemana().isEmpty());

        // Comparative
        assertFalse(res.getComparativa().isEmpty());
        assertEquals(15, res.getComparativa().size()); // 15 days

        // Period labels
        assertNotNull(res.getPeriodo());
        assertNotNull(res.getPeriodoAnterior());
    }

    @Test
    void advancedReport_WhenResumenFails_ShouldReturnErrorStatus() {
        // Given — only sumTotalByCreatedAtBetween throws, so resumen fails
        when(saleRepository.sumTotalByCreatedAtBetween(any(), any()))
                .thenThrow(new RuntimeException("DB Error"));

        // When
        SalesReportResponse res = salesReportService.advancedReport(desde, hasta);

        // Then
        assertNotNull(res);
        assertEquals("ERROR", res.getResumen().getStatus());
        // Other sections still work independently (may be non-empty if unstubbed returns empty list)
    }

    // ── Stubs ──────────────────────────────────────────────────────

    private void stubBasicMetrics() {
        when(saleRepository.sumTotalByCreatedAtBetween(any(), any()))
                .thenReturn(BigDecimal.valueOf(15000));
        when(saleRepository.countByStatusAndCreatedAtBetween(eq(SaleStatus.COMPLETED), any(), any()))
                .thenReturn(30L);
        when(saleRepository.sumRefundsByCreatedAtBetween(any(), any()))
                .thenReturn(BigDecimal.valueOf(500));
        when(saleRepository.sumDiscountsByCreatedAtBetween(any(), any()))
                .thenReturn(BigDecimal.valueOf(200));
        when(saleRepository.sumTaxesByCreatedAtBetween(any(), any()))
                .thenReturn(BigDecimal.valueOf(3000));
    }

    private void stubSalesByPaymentMethod() {
        java.util.List<Object[]> data = new java.util.ArrayList<>();
        data.add(new Object[]{"CASH", BigDecimal.valueOf(10000), 20L});
        data.add(new Object[]{"DEBIT_CARD", BigDecimal.valueOf(5000), 10L});
        when(saleRepository.findSalesByPaymentMethod(any(), any())).thenReturn(data);
    }

    private void stubSalesByHour() {
        java.util.List<Object[]> data = new java.util.ArrayList<>();
        data.add(new Object[]{10, BigDecimal.valueOf(2000), 5L});
        data.add(new Object[]{11, BigDecimal.valueOf(3000), 8L});
        data.add(new Object[]{12, BigDecimal.valueOf(5000), 12L});
        when(saleRepository.findSalesByHour(any(), any())).thenReturn(data);
    }

    private void stubSalesByDayOfWeek() {
        java.util.List<Object[]> data = new java.util.ArrayList<>();
        data.add(new Object[]{1, BigDecimal.valueOf(3000), 6L});
        data.add(new Object[]{2, BigDecimal.valueOf(4000), 8L});
        data.add(new Object[]{3, BigDecimal.valueOf(5000), 10L});
        when(saleRepository.findSalesByDayOfWeek(any(), any())).thenReturn(data);
    }

    private void stubDailySales() {
        java.util.List<Object[]> data = new java.util.ArrayList<>();
        for (int i = 0; i < 15; i++) {
            data.add(new Object[]{java.sql.Date.valueOf(desde.plusDays(i)), BigDecimal.valueOf(1000)});
        }
        when(saleRepository.findDailySalesTotals(any(), any())).thenReturn(data);
    }
}
