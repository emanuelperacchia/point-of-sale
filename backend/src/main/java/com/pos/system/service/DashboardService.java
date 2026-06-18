package com.pos.system.service;

import com.pos.system.dto.response.ExecutiveDashboardResponse;
import com.pos.system.dto.response.ExecutiveDashboardResponse.*;
import com.pos.system.entity.ProductionOrder;
import com.pos.system.entity.Receivable;
import com.pos.system.entity.SaleStatus;
import com.pos.system.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Orquesta el Dashboard Ejecutivo con consultas paralelizadas.
 * Cachea resultado por 5 minutos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final long DIAS_PERIODO_DEFAULT = 30;

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;
    private final ReceivableRepository receivableRepository;
    private final ExpenseRepository expenseRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final PayrollRepository payrollRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final ProductionOrderComponentRepository productionOrderComponentRepository;
    private final EmployeeRepository employeeRepository;
    private final AlertService alertService;

    /**
     * Obtiene el resumen ejecutivo completo.
     */
    @Cacheable(value = "executiveDashboard", key = "#periodo + '-' + #sucursalId", unless = "#result == null")
    public ExecutiveDashboardResponse getExecutiveSummary(String periodo, Long sucursalId) {
        PeriodoInfo p = parsePeriodo(periodo);
        PeriodoInfo pAnterior = periodoAnterior(p);

        // Lanzar todas las consultas en paralelo
        CompletableFuture<SalesKPI> salesFuture = CompletableFuture.supplyAsync(() ->
                buildSalesKPI(p, pAnterior));

        CompletableFuture<InventoryKPI> inventoryFuture = CompletableFuture.supplyAsync(() ->
                buildInventoryKPI(sucursalId));

        CompletableFuture<FinancialKPI> financialFuture = CompletableFuture.supplyAsync(() ->
                buildFinancialKPI(p, pAnterior, sucursalId));

        CompletableFuture<HRKPI> hrFuture = CompletableFuture.supplyAsync(() ->
                buildHRKPI(p));

        CompletableFuture<ProductionKPI> productionFuture = CompletableFuture.supplyAsync(() ->
                buildProductionKPI(p));

        CompletableFuture<List<DailySalesPoint>> dailySalesFuture = CompletableFuture.supplyAsync(() ->
                buildDailySales(p));

        CompletableFuture<List<ProductRankingItem>> topProductsFuture = CompletableFuture.supplyAsync(() ->
                buildTopProducts(p, pAnterior));

        CompletableFuture<List<SellerRankingItem>> topSellersFuture = CompletableFuture.supplyAsync(() ->
                buildTopSellers(p, pAnterior));

        CompletableFuture<List<DashboardAlert>> alertsFuture = CompletableFuture.supplyAsync(() ->
                buildAlerts(sucursalId));

        // Ensamblar respuesta
        try {
            return ExecutiveDashboardResponse.builder()
                    .sales(salesFuture.get())
                    .inventory(inventoryFuture.get())
                    .financial(financialFuture.get())
                    .hr(hrFuture.get())
                    .production(productionFuture.get())
                    .dailySales(dailySalesFuture.get())
                    .topProducts(topProductsFuture.get())
                    .topSellers(topSellersFuture.get())
                    .alerts(alertsFuture.get())
                    .build();
        } catch (Exception e) {
            log.error("Error construyendo dashboard ejecutivo", e);
            throw new RuntimeException("Error al generar dashboard ejecutivo", e);
        }
    }

    // ── KPIs ───────────────────────────────────────────────────────

    private SalesKPI buildSalesKPI(PeriodoInfo p, PeriodoInfo pAnt) {
        try {
            BigDecimal totalActual = saleRepository.sumTotalByCreatedAtBetween(p.desdeDt(), p.hastaDt());
            BigDecimal totalAnterior = saleRepository.sumTotalByCreatedAtBetween(pAnt.desdeDt(), pAnt.hastaDt());
            long transActual = saleRepository.countByStatusAndCreatedAtBetween(SaleStatus.COMPLETED, p.desdeDt(), p.hastaDt());
            long transAnterior = saleRepository.countByStatusAndCreatedAtBetween(SaleStatus.COMPLETED, pAnt.desdeDt(), pAnt.hastaDt());

            BigDecimal avgTicket = transActual > 0
                    ? totalActual.divide(BigDecimal.valueOf(transActual), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal salesVar = calcularVariacion(totalActual, totalAnterior);
            BigDecimal transVar = calcularVariacion(BigDecimal.valueOf(transActual), BigDecimal.valueOf(transAnterior));

            return SalesKPI.builder()
                    .totalSales(totalActual)
                    .transactionCount(transActual)
                    .averageTicket(avgTicket)
                    .salesVariation(salesVar)
                    .transactionVariation(transVar)
                    .status("OK")
                    .build();
        } catch (Exception e) {
            log.warn("Error en sales KPI", e);
            return SalesKPI.builder().status("ERROR").build();
        }
    }

    private InventoryKPI buildInventoryKPI(Long sucursalId) {
        try {
            BigDecimal stockValue = productStockRepository.getTotalInventoryValueGlobal();
            long criticalCount = productStockRepository.countProductsBelowMinimum();

            // Productos sin movimiento en los últimos 90 días
            LocalDate threshold = LocalDate.now().minusDays(90);
            long activeProducts = productRepository.countByActiveTrue();
            long noMovementCount = activeProducts; // placeholder — idealmente cruzar con sale_items

            return InventoryKPI.builder()
                    .totalStockValue(stockValue)
                    .criticalStockCount(criticalCount)
                    .noMovementCount(noMovementCount)
                    .status("OK")
                    .build();
        } catch (Exception e) {
            log.warn("Error en inventory KPI", e);
            return InventoryKPI.builder().status("ERROR").build();
        }
    }

    private FinancialKPI buildFinancialKPI(PeriodoInfo p, PeriodoInfo pAnt, Long sucursalId) {
        try {
            BigDecimal income = saleRepository.sumTotalByCreatedAtBetween(p.desdeDt(), p.hastaDt());
            BigDecimal expenses = expenseRepository.sumExpensesByPeriod(p.desde(), p.hasta());
            BigDecimal overdue = receivableRepository.sumOverdueReceivables();
            BigDecimal projected = income.subtract(expenses);

            return FinancialKPI.builder()
                    .income(income)
                    .expenses(expenses)
                    .projectedBalance(projected)
                    .overdueReceivables(overdue)
                    .status("OK")
                    .build();
        } catch (Exception e) {
            log.warn("Error en financial KPI", e);
            return FinancialKPI.builder().status("ERROR").build();
        }
    }

    private HRKPI buildHRKPI(PeriodoInfo p) {
        try {
            long activeEmployees = employeeRepository.findByActivoTrue().size();

            // Labor cost: suma del mes actual
            LocalDate now = LocalDate.now();
            BigDecimal laborCost = payrollRepository.sumNetoByMesAndAnio(now.getMonthValue(), now.getYear());

            // Ausentismo: días AUSENCIA / días laborables
            long ausencias = attendanceRecordRepository.countAusenciasByPeriod(p.desde(), p.hasta());
            long totalDiasLaborables = ChronoUnit.DAYS.between(p.desde(), p.hasta()) * 5 / 7;
            totalDiasLaborables = Math.max(totalDiasLaborables, 1);
            BigDecimal absenteeismRate = BigDecimal.valueOf(ausencias)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalDiasLaborables * Math.max(activeEmployees, 1)), 2, RoundingMode.HALF_UP);

            return HRKPI.builder()
                    .activeEmployees(activeEmployees)
                    .absenteeismRate(absenteeismRate)
                    .laborCost(laborCost)
                    .status("OK")
                    .build();
        } catch (Exception e) {
            log.warn("Error en HR KPI", e);
            return HRKPI.builder().status("ERROR").build();
        }
    }

    private ProductionKPI buildProductionKPI(PeriodoInfo p) {
        try {
            long completedOrders = productionOrderRepository.countByEstadoAndCreatedAtBetween(
                    ProductionOrder.Estado.COMPLETADA, p.desdeDt(), p.hastaDt());

            // Merma promedio
            List<Object[]> wasteData = productionOrderComponentRepository.findAverageWasteByPeriod(p.desdeDt(), p.hastaDt());
            BigDecimal avgWaste = BigDecimal.ZERO;
            if (!wasteData.isEmpty() && wasteData.get(0)[0] != null) {
                avgWaste = ((Number) wasteData.get(0)[0]).doubleValue() >= 0
                        ? BigDecimal.valueOf(((Number) wasteData.get(0)[0]).doubleValue())
                        : BigDecimal.ZERO;
            }

            return ProductionKPI.builder()
                    .completedOrders(completedOrders)
                    .averageWaste(avgWaste)
                    .averageProductionCost(BigDecimal.ZERO) // simplificado
                    .status("OK")
                    .build();
        } catch (Exception e) {
            log.warn("Error en production KPI", e);
            return ProductionKPI.builder().status("ERROR").build();
        }
    }

    // ── Charts ─────────────────────────────────────────────────────

    private List<DailySalesPoint> buildDailySales(PeriodoInfo p) {
        List<Object[]> raw = saleRepository.findDailySalesTotals(p.desdeDt(), p.hastaDt());
        return raw.stream()
                .map(row -> DailySalesPoint.builder()
                        .date(row[0] != null ? row[0].toString() : "")
                        .amount(row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());
    }

    // ── Rankings ───────────────────────────────────────────────────

    private List<ProductRankingItem> buildTopProducts(PeriodoInfo p, PeriodoInfo pAnt) {
        List<Object[]> actual = saleRepository.findTopProducts(p.desdeDt(), p.hastaDt());
        List<Object[]> anterior = saleRepository.findTopProducts(pAnt.desdeDt(), pAnt.hastaDt());

        // Mapa de montos anteriores por productId
        java.util.Map<Long, BigDecimal> anteriorMap = anterior.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> BigDecimal.valueOf(((Number) row[3]).doubleValue()),
                        (a, b) -> a));

        return actual.stream()
                .map(row -> {
                    Long productId = ((Number) row[0]).longValue();
                    BigDecimal prevAmount = anteriorMap.getOrDefault(productId, BigDecimal.ZERO);
                    BigDecimal currentAmount = BigDecimal.valueOf(((Number) row[3]).doubleValue());

                    return ProductRankingItem.builder()
                            .productId(productId)
                            .productName((String) row[1])
                            .productSku((String) row[2])
                            .totalAmount(currentAmount)
                            .quantity(BigDecimal.valueOf(((Number) row[4]).doubleValue()))
                            .variation(calcularVariacion(currentAmount, prevAmount))
                            .build();
                })
                .limit(5)
                .collect(Collectors.toList());
    }

    private List<SellerRankingItem> buildTopSellers(PeriodoInfo p, PeriodoInfo pAnt) {
        List<Object[]> actual = saleRepository.findTopSellers(p.desdeDt(), p.hastaDt());
        List<Object[]> anterior = saleRepository.findTopSellers(pAnt.desdeDt(), pAnt.hastaDt());

        java.util.Map<Long, BigDecimal> anteriorMap = anterior.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> BigDecimal.valueOf(((Number) row[2]).doubleValue()),
                        (a, b) -> a));

        return actual.stream()
                .map(row -> {
                    Long userId = ((Number) row[0]).longValue();
                    BigDecimal prevAmount = anteriorMap.getOrDefault(userId, BigDecimal.ZERO);
                    BigDecimal currentAmount = BigDecimal.valueOf(((Number) row[2]).doubleValue());

                    return SellerRankingItem.builder()
                            .employeeId(userId)
                            .employeeName((String) row[1])
                            .totalAmount(currentAmount)
                            .transactionCount(((Number) row[3]).longValue())
                            .variation(calcularVariacion(currentAmount, prevAmount))
                            .build();
                })
                .limit(5)
                .collect(Collectors.toList());
    }

    // ── Alertas ────────────────────────────────────────────────────

    private List<DashboardAlert> buildAlerts(Long sucursalId) {
        List<DashboardAlert> alerts = new ArrayList<>();

        try {
            // Stock crítico
            long criticalCount = productStockRepository.countProductsBelowMinimum();
            if (criticalCount > 0) {
                alerts.add(DashboardAlert.builder()
                        .type("CRITICAL_STOCK")
                        .severity("HIGH")
                        .message(criticalCount + " productos con stock por debajo del mínimo")
                        .actionLink("/inventory")
                        .count((int) criticalCount)
                        .build());
            }
        } catch (Exception e) {
            log.warn("Error generando alerta de stock", e);
        }

        try {
            // Cuentas por cobrar vencidas
            BigDecimal overdue = receivableRepository.sumOverdueReceivables();
            if (overdue.compareTo(BigDecimal.ZERO) > 0) {
                alerts.add(DashboardAlert.builder()
                        .type("OVERDUE_RECEIVABLE")
                        .severity("MEDIUM")
                        .message("Cuentas por cobrar vencidas: $" + overdue)
                        .actionLink("/receivables")
                        .count(overdue.intValue())
                        .build());
            }
        } catch (Exception e) {
            log.warn("Error generando alerta de cobranza", e);
        }

        return alerts;
    }

    // ── Helpers ────────────────────────────────────────────────────

    private BigDecimal calcularVariacion(BigDecimal actual, BigDecimal anterior) {
        if (anterior.compareTo(BigDecimal.ZERO) == 0) {
            return actual.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return actual.subtract(anterior)
                .multiply(BigDecimal.valueOf(100))
                .divide(anterior, 2, RoundingMode.HALF_UP);
    }

    /**
     * Parsea un período en formato string a fechas.
     * Soporta: TODAY, YESTERDAY, WEEK, MONTH, YTD, CUSTOM:yyyy-MM-dd:yyyy-MM-dd
     */
    static PeriodoInfo parsePeriodo(String periodo) {
        if (periodo == null) periodo = "MONTH";

        LocalDate hoy = LocalDate.now();
        return switch (periodo.toUpperCase()) {
            case "TODAY" -> new PeriodoInfo(hoy, hoy);
            case "YESTERDAY" -> new PeriodoInfo(hoy.minusDays(1), hoy.minusDays(1));
            case "WEEK" -> new PeriodoInfo(hoy.minusDays(6), hoy);
            case "MONTH" -> new PeriodoInfo(hoy.withDayOfMonth(1), hoy);
            case "YTD" -> new PeriodoInfo(hoy.withDayOfYear(1), hoy);
            default -> {
                if (periodo.toUpperCase().startsWith("CUSTOM:")) {
                    String[] parts = periodo.split(":", 3);
                    if (parts.length == 3) {
                        yield new PeriodoInfo(LocalDate.parse(parts[1]), LocalDate.parse(parts[2]));
                    }
                }
                yield new PeriodoInfo(hoy.withDayOfMonth(1), hoy);
            }
        };
    }

    /**
     * Calcula el período anterior de la misma duración.
     */
    static PeriodoInfo periodoAnterior(PeriodoInfo actual) {
        long dias = ChronoUnit.DAYS.between(actual.desde(), actual.hasta()) + 1;
        LocalDate antHasta = actual.desde().minusDays(1);
        LocalDate antDesde = antHasta.minusDays(dias - 1);
        return new PeriodoInfo(antDesde, antHasta);
    }

    /**
     * Período de tiempo con soporte LocalDate y LocalDateTime.
     */
    public record PeriodoInfo(LocalDate desde, LocalDate hasta) {
        public LocalDateTime desdeDt() { return desde.atStartOfDay(); }
        public LocalDateTime hastaDt() { return hasta.atTime(LocalTime.MAX); }
    }
}
