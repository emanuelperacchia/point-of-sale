package com.pos.system.service;

import com.pos.system.dto.response.ExecutiveDashboardResponse;
import com.pos.system.entity.ProductionOrder;
import com.pos.system.entity.SaleStatus;
import com.pos.system.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private SaleRepository saleRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductStockRepository productStockRepository;
    @Mock private ReceivableRepository receivableRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private AttendanceRecordRepository attendanceRecordRepository;
    @Mock private PayrollRepository payrollRepository;
    @Mock private ProductionOrderRepository productionOrderRepository;
    @Mock private ProductionOrderComponentRepository productionOrderComponentRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private AlertService alertService;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                saleRepository, productRepository, productStockRepository,
                receivableRepository, expenseRepository, attendanceRecordRepository,
                payrollRepository, productionOrderRepository, productionOrderComponentRepository,
                employeeRepository, alertService);
    }

    // ── parsePeriodo ───────────────────────────────────────────────

    @Test
    void parsePeriodo_ShouldReturnToday() {
        var p = DashboardService.parsePeriodo("TODAY");
        assertEquals(LocalDate.now(), p.desde());
        assertEquals(LocalDate.now(), p.hasta());
    }

    @Test
    void parsePeriodo_ShouldReturnYesterday() {
        var p = DashboardService.parsePeriodo("YESTERDAY");
        assertEquals(LocalDate.now().minusDays(1), p.desde());
        assertEquals(LocalDate.now().minusDays(1), p.hasta());
    }

    @Test
    void parsePeriodo_ShouldReturnWeek() {
        var p = DashboardService.parsePeriodo("WEEK");
        assertEquals(LocalDate.now().minusDays(6), p.desde());
        assertEquals(LocalDate.now(), p.hasta());
    }

    @Test
    void parsePeriodo_ShouldReturnMonth() {
        var p = DashboardService.parsePeriodo("MONTH");
        assertEquals(LocalDate.now().withDayOfMonth(1), p.desde());
        assertEquals(LocalDate.now(), p.hasta());
    }

    @Test
    void parsePeriodo_ShouldReturnYTD() {
        var p = DashboardService.parsePeriodo("YTD");
        assertEquals(LocalDate.now().withDayOfYear(1), p.desde());
        assertEquals(LocalDate.now(), p.hasta());
    }

    @Test
    void parsePeriodo_ShouldReturnCustom() {
        var p = DashboardService.parsePeriodo("CUSTOM:2026-01-01:2026-01-31");
        assertEquals(LocalDate.of(2026, 1, 1), p.desde());
        assertEquals(LocalDate.of(2026, 1, 31), p.hasta());
    }

    @Test
    void parsePeriodo_ShouldDefaultToMonthForInvalid() {
        var p = DashboardService.parsePeriodo("INVALIDO");
        assertEquals(LocalDate.now().withDayOfMonth(1), p.desde());
        assertEquals(LocalDate.now(), p.hasta());
    }

    @Test
    void parsePeriodo_ShouldDefaultToMonthForNull() {
        var p = DashboardService.parsePeriodo(null);
        assertEquals(LocalDate.now().withDayOfMonth(1), p.desde());
    }

    // ── periodoAnterior ────────────────────────────────────────────

    @Test
    void periodoAnterior_ShouldShiftSameDuration() {
        var actual = new DashboardService.PeriodoInfo(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 15));

        var anterior = DashboardService.periodoAnterior(actual);

        assertEquals(LocalDate.of(2026, 5, 17), anterior.desde());
        assertEquals(LocalDate.of(2026, 5, 31), anterior.hasta());
    }

    @Test
    void periodoAnterior_OneDayPeriod() {
        var actual = new DashboardService.PeriodoInfo(
                LocalDate.of(2026, 6, 15),
                LocalDate.of(2026, 6, 15));

        var anterior = DashboardService.periodoAnterior(actual);

        assertEquals(LocalDate.of(2026, 6, 14), anterior.desde());
        assertEquals(LocalDate.of(2026, 6, 14), anterior.hasta());
    }

    // ── getExecutiveSummary ────────────────────────────────────────

    @Test
    void getExecutiveSummary_ShouldReturnCompleteDashboard() {
        // Given
        stubSales();
        stubInventory();
        stubFinancial();
        stubHR();
        stubProduction();
        stubDailySales();
        stubTopProducts();
        stubTopSellers();
        stubAlerts();

        // When
        ExecutiveDashboardResponse response = dashboardService.getExecutiveSummary("MONTH", null);

        // Then
        assertNotNull(response);
        assertNotNull(response.getSales());
        assertNotNull(response.getInventory());
        assertNotNull(response.getFinancial());
        assertNotNull(response.getHr());
        assertNotNull(response.getProduction());
        assertNotNull(response.getDailySales());
        assertNotNull(response.getTopProducts());
        assertNotNull(response.getTopSellers());
        assertNotNull(response.getAlerts());

        // Sales KPI
        assertEquals(0, BigDecimal.valueOf(5000).compareTo(response.getSales().getTotalSales()));
        assertEquals(10, response.getSales().getTransactionCount());
        assertEquals(0, BigDecimal.valueOf(500).compareTo(response.getSales().getAverageTicket()));
        assertEquals("OK", response.getSales().getStatus());

        // Inventory KPI
        assertTrue(response.getInventory().getTotalStockValue().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(response.getInventory().getStatus().equals("OK"));

        // Financial
        assertTrue(response.getFinancial().getIncome().compareTo(BigDecimal.ZERO) > 0);

        // HR
        assertTrue(response.getHr().getActiveEmployees() > 0);
        assertEquals("OK", response.getHr().getStatus());

        // Production
        assertTrue(response.getProduction().getCompletedOrders() >= 0);
        assertEquals("OK", response.getProduction().getStatus());

        // Rankings
        assertFalse(response.getTopProducts().isEmpty());
        assertFalse(response.getTopSellers().isEmpty());
        assertFalse(response.getAlerts().isEmpty());

        // Daily sales
        assertFalse(response.getDailySales().isEmpty());
        assertEquals("2026-06-01", response.getDailySales().get(0).getDate());
    }

    @Test
    void getExecutiveSummary_WhenSalesFail_ShouldReturnErrorStatus() {
        // Given — make ALL sumTotalByCreatedAtBetween calls fail
        when(saleRepository.sumTotalByCreatedAtBetween(any(), any()))
                .thenThrow(new RuntimeException("DB Error"));
        // The financial KPI also calls sumTotalByCreatedAtBetween, so it fails too

        stubInventory();
        stubHR();
        stubProduction();
        stubDailySales();
        stubTopProducts();
        stubTopSellers();
        stubAlerts();
        // Financial stub NOT needed — it will also fail due to sales repo error

        // When
        ExecutiveDashboardResponse response = dashboardService.getExecutiveSummary("MONTH", null);

        // Then
        assertNotNull(response);
        assertEquals("ERROR", response.getSales().getStatus());
        assertEquals("ERROR", response.getFinancial().getStatus());
        assertEquals("OK", response.getInventory().getStatus());
    }

    // ── Stubs ──────────────────────────────────────────────────────

    private void stubSales() {
        when(saleRepository.sumTotalByCreatedAtBetween(any(), any()))
                .thenReturn(BigDecimal.valueOf(5000));
        when(saleRepository.countByStatusAndCreatedAtBetween(eq(SaleStatus.COMPLETED), any(), any()))
                .thenReturn(10L);
        // No prior period results for simplicity
    }

    private void stubInventory() {
        when(productStockRepository.getTotalInventoryValueGlobal())
                .thenReturn(BigDecimal.valueOf(25000));
        when(productStockRepository.countProductsBelowMinimum())
                .thenReturn(3L);
        when(productRepository.countByActiveTrue()).thenReturn(100L);
    }

    private void stubFinancial() {
        when(expenseRepository.sumExpensesByPeriod(any(), any()))
                .thenReturn(BigDecimal.valueOf(2000));
        when(receivableRepository.sumOverdueReceivables())
                .thenReturn(BigDecimal.valueOf(1500));
    }

    private void stubHR() {
        var emp1 = com.pos.system.entity.Employee.builder()
                .id(1L).nombre("Juan").apellido("Perez").activo(true).build();
        when(employeeRepository.findByActivoTrue()).thenReturn(List.of(emp1));
        when(payrollRepository.sumNetoByMesAndAnio(anyInt(), anyInt()))
                .thenReturn(BigDecimal.valueOf(10000));
        when(attendanceRecordRepository.countAusenciasByPeriod(any(), any()))
                .thenReturn(2L);
    }

    private void stubProduction() {
        when(productionOrderRepository.countByEstadoAndCreatedAtBetween(
                eq(ProductionOrder.Estado.COMPLETADA), any(), any()))
                .thenReturn(5L);
        java.util.List<Object[]> wasteResult = new java.util.ArrayList<>();
        wasteResult.add(new Object[]{BigDecimal.valueOf(2.5)});
        when(productionOrderComponentRepository.findAverageWasteByPeriod(any(), any()))
                .thenReturn(wasteResult);
    }

    private void stubDailySales() {
        java.util.List<Object[]> dailyResult = new java.util.ArrayList<>();
        dailyResult.add(new Object[]{java.sql.Date.valueOf("2026-06-01"), BigDecimal.valueOf(1000)});
        dailyResult.add(new Object[]{java.sql.Date.valueOf("2026-06-02"), BigDecimal.valueOf(2000)});
        when(saleRepository.findDailySalesTotals(any(), any()))
                .thenReturn(dailyResult);
    }

    private void stubTopProducts() {
        java.util.List<Object[]> topProducts = new java.util.ArrayList<>();
        topProducts.add(new Object[]{1L, "Producto A", "SKU001", BigDecimal.valueOf(3000), BigDecimal.TEN});
        topProducts.add(new Object[]{2L, "Producto B", "SKU002", BigDecimal.valueOf(2000), BigDecimal.valueOf(5)});
        when(saleRepository.findTopProducts(any(), any()))
                .thenReturn(topProducts);
    }

    private void stubTopSellers() {
        java.util.List<Object[]> topSellers = new java.util.ArrayList<>();
        topSellers.add(new Object[]{1L, "Juan Perez", BigDecimal.valueOf(3000), 8L});
        topSellers.add(new Object[]{2L, "Maria Lopez", BigDecimal.valueOf(2000), 5L});
        when(saleRepository.findTopSellers(any(), any()))
                .thenReturn(topSellers);
    }

    private void stubAlerts() {
        when(productStockRepository.countProductsBelowMinimum()).thenReturn(3L);
        when(receivableRepository.sumOverdueReceivables()).thenReturn(BigDecimal.valueOf(1500));
    }
}
