package com.pos.system.service;

import com.pos.system.dto.response.CashFlowDayRow;
import com.pos.system.dto.response.CashFlowResponse;
import com.pos.system.repository.ExpenseRepository;
import com.pos.system.repository.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashFlowServiceTest {

    @Mock private SaleRepository saleRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private PayableService payableService;

    private CashFlowService cashFlowService;

    private final LocalDate desde = LocalDate.of(2026, 6, 1);
    private final LocalDate hasta = LocalDate.of(2026, 6, 5);

    @BeforeEach
    void setUp() {
        cashFlowService = new CashFlowService(saleRepository, expenseRepository, payableService);
    }

    @Test
    void getCashFlow_WithoutProyeccion_ShouldReturnRealDays() {
        // Given
        stubIngresosReales(Collections.singletonList(
                row(java.sql.Date.valueOf(desde), BigDecimal.valueOf(1000))
        ));
        stubEgresosReales(Collections.singletonList(
                row(java.sql.Date.valueOf(desde.plusDays(1)), BigDecimal.valueOf(500))
        ));

        // When
        CashFlowResponse response = cashFlowService.getCashFlow(desde, hasta, false, 0);

        // Then
        assertNotNull(response);
        assertFalse(response.alertaSaldoNegativo());
        assertNull(response.primerDiaSaldoNegativo());

        assertEquals(5, response.dias().size());

        CashFlowDayRow day1 = response.dias().get(0);
        assertEquals(desde, day1.fecha());
        assertFalse(day1.esProyectado());
        assertEquals(0, BigDecimal.valueOf(1000).compareTo(day1.ingresos()));
        assertEquals(0, BigDecimal.ZERO.compareTo(day1.egresos()));
        assertEquals(0, BigDecimal.valueOf(1000).compareTo(day1.saldoAcumulado()));

        CashFlowDayRow day2 = response.dias().get(1);
        assertEquals(0, BigDecimal.valueOf(500).compareTo(day2.egresos()));
        assertEquals(0, BigDecimal.valueOf(500).compareTo(day2.saldoAcumulado()));
    }

    @Test
    void getCashFlow_WithProyeccion_ShouldIncludeProjectedDays() {
        // Given
        int diasProyeccion = 3;
        LocalDate inicioPromedio = desde.minusDays(90);

        stubIngresosHistoricos(inicioPromedio, desde, BigDecimal.valueOf(1000));
        stubEgresosHistoricos(inicioPromedio, desde, BigDecimal.valueOf(500));
        stubPayables();

        stubIngresosReales(List.of());
        stubEgresosReales(List.of());
        stubEgresosFuturos(hasta, diasProyeccion, List.of());

        // When
        CashFlowResponse response = cashFlowService.getCashFlow(desde, hasta, true, diasProyeccion);

        // Then
        assertNotNull(response);
        assertEquals(5 + diasProyeccion, response.dias().size());

        CashFlowDayRow firstProjected = response.dias().get(5);
        assertTrue(firstProjected.esProyectado());
        assertEquals(hasta.plusDays(1), firstProjected.fecha());
    }

    @Test
    void getCashFlow_WithNegativeSaldo_ShouldSetAlerta() {
        // Given
        int diasProyeccion = 3;

        stubIngresosHistoricos(desde.minusDays(90), desde, BigDecimal.ZERO);
        stubEgresosHistoricos(desde.minusDays(90), desde, BigDecimal.ZERO);
        stubPayables();

        stubIngresosReales(List.of());
        stubEgresosReales(Collections.singletonList(
                row(java.sql.Date.valueOf(desde), BigDecimal.valueOf(100_000))
        ));
        stubEgresosFuturos(hasta, diasProyeccion, List.of());

        // When
        CashFlowResponse response = cashFlowService.getCashFlow(desde, hasta, true, diasProyeccion);

        // Then: real day has -100k, projection continues with 0/0 → stays negative
        assertTrue(response.alertaSaldoNegativo());
        assertNotNull(response.primerDiaSaldoNegativo());
    }

    @Test
    void getCashFlow_WithProyeccionNegative_ShouldSetAlertaOnProjectedDay() {
        // Given
        int diasProyeccion = 5;
        LocalDate inicioPromedio = desde.minusDays(90);

        stubIngresosHistoricos(inicioPromedio, desde, BigDecimal.valueOf(500));
        stubEgresosHistoricos(inicioPromedio, desde, BigDecimal.valueOf(1000));
        stubPayables();

        stubIngresosReales(List.of());
        stubEgresosReales(List.of());
        stubEgresosFuturos(hasta, diasProyeccion, List.of());

        // When
        CashFlowResponse response = cashFlowService.getCashFlow(desde, hasta, true, diasProyeccion);

        // Then: negative average should make projection negative
        assertTrue(response.alertaSaldoNegativo());
        assertNotNull(response.primerDiaSaldoNegativo());
        assertTrue(response.primerDiaSaldoNegativo().isAfter(hasta));
    }

    @Test
    void getCashFlow_WithFutureRecurrentes_ShouldOverridePromedio() {
        // Given
        int diasProyeccion = 2;
        LocalDate inicioPromedio = desde.minusDays(90);
        LocalDate futureDate = hasta.plusDays(1);

        stubIngresosHistoricos(inicioPromedio, desde, BigDecimal.valueOf(5000));
        stubEgresosHistoricos(inicioPromedio, desde, BigDecimal.ZERO);
        stubPayables();

        stubIngresosReales(List.of());
        stubEgresosReales(List.of());

        stubEgresosFuturos(hasta, diasProyeccion,
                Collections.singletonList(
                        row(java.sql.Date.valueOf(futureDate), BigDecimal.valueOf(1_000_000))
                ));

        // When
        CashFlowResponse response = cashFlowService.getCashFlow(desde, hasta, true, diasProyeccion);

        // Then
        CashFlowDayRow projectedDay = response.dias().get(5);
        assertEquals(0, BigDecimal.valueOf(1_000_000).compareTo(projectedDay.egresos()));
        assertTrue(response.alertaSaldoNegativo());
    }

    @Test
    void getCashFlow_WhenInvertedDates_ShouldHandleGracefully() {
        LocalDate invertedDesde = LocalDate.of(2026, 6, 10);
        int diasProyeccion = 2;

        when(saleRepository.findDailySalesTotals(any(), any())).thenReturn(List.of());
        when(expenseRepository.findDailyExpenseTotals(any(), any())).thenReturn(List.of());
        when(payableService.totalPendienteEntreFechas(any(), any())).thenReturn(BigDecimal.ZERO);

        CashFlowResponse response = cashFlowService.getCashFlow(hasta, invertedDesde, true, diasProyeccion);

        assertNotNull(response);
        assertFalse(response.alertaSaldoNegativo());
    }

    @Test
    void getCashFlow_WithNullDesde_ShouldThrow() {
        assertThrows(NullPointerException.class, () ->
                cashFlowService.getCashFlow(null, hasta, false, 0));
    }

    // ── Helper stubs ────────────────────────────────────────────────────

    private void stubIngresosReales(List<Object[]> data) {
        when(saleRepository.findDailySalesTotals(
                desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay()))
                .thenReturn(data);
    }

    private void stubEgresosReales(List<Object[]> data) {
        when(expenseRepository.findDailyExpenseTotals(desde, hasta.plusDays(1)))
                .thenReturn(data);
    }

    private void stubIngresosHistoricos(LocalDate from, LocalDate to, BigDecimal dailyAmount) {
        when(saleRepository.findDailySalesTotals(
                from.atStartOfDay(), to.atStartOfDay()))
                .thenReturn(Collections.singletonList(
                        new Object[]{java.sql.Date.valueOf(from), dailyAmount}
                ));
    }

    private void stubEgresosHistoricos(LocalDate from, LocalDate to, BigDecimal dailyAmount) {
        when(expenseRepository.findDailyExpenseTotals(from, to))
                .thenReturn(Collections.singletonList(
                        new Object[]{java.sql.Date.valueOf(from), dailyAmount}
                ));
    }

    private void stubEgresosFuturos(LocalDate hastaRef, int diasProyeccion, List<Object[]> data) {
        when(expenseRepository.findDailyExpenseTotals(
                hastaRef.plusDays(1), hastaRef.plusDays(diasProyeccion + 1)))
                .thenReturn(data);
    }

    private void stubPayables() {
        when(payableService.totalPendienteEntreFechas(any(), any())).thenReturn(BigDecimal.ZERO);
    }

    private Object[] row(java.sql.Date fecha, BigDecimal total) {
        return new Object[]{fecha, total};
    }
}
