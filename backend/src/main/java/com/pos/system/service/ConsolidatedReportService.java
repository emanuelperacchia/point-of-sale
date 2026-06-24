package com.pos.system.service;

import com.pos.system.dto.response.ConsolidatedReportResponse;
import com.pos.system.dto.response.ExecutiveDashboardResponse;
import com.pos.system.entity.Branch;
import com.pos.system.entity.StockTransfer;
import com.pos.system.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsolidatedReportService {

    private final BranchRepository branchRepository;
    private final DashboardService dashboardService;
    private final StockTransferRepository stockTransferRepository;

    @Transactional(readOnly = true)
    public ConsolidatedReportResponse getSummary(LocalDate desde, LocalDate hasta) {
        try {
            List<Branch> activeBranches = branchRepository.findByActivaTrue();
            String periodo = "30d";

            // Ejecutar dashboard de cada sucursal en paralelo
            Map<Long, CompletableFuture<ExecutiveDashboardResponse>> futures = new HashMap<>();
            for (Branch branch : activeBranches) {
                futures.put(branch.getId(),
                        CompletableFuture.supplyAsync(() ->
                                dashboardService.getExecutiveSummary(periodo, branch.getId())));
            }

            // Resolver todos los futures
            Map<Long, ExecutiveDashboardResponse> dashboards = new HashMap<>();
            for (Map.Entry<Long, CompletableFuture<ExecutiveDashboardResponse>> entry : futures.entrySet()) {
                try {
                    dashboards.put(entry.getKey(), entry.getValue().get());
                } catch (Exception e) {
                    log.warn("Error obteniendo dashboard de sucursal {}: {}", entry.getKey(), e.getMessage());
                }
            }

            // Totales globales
            BigDecimal ventasGlobal = BigDecimal.ZERO;
            long ordenesProduccion = 0;

            for (ExecutiveDashboardResponse dash : dashboards.values()) {
                if (dash == null) continue;
                ExecutiveDashboardResponse.SalesKPI sales = dash.getSales();
                if (sales != null && "OK".equals(sales.getStatus())) {
                    ventasGlobal = ventasGlobal.add(sales.getTotalSales() != null ? sales.getTotalSales() : BigDecimal.ZERO);
                }
                ExecutiveDashboardResponse.ProductionKPI prod = dash.getProduction();
                if (prod != null && prod.getCompletedOrders() != null) {
                    ordenesProduccion += prod.getCompletedOrders();
                }
            }

            BigDecimal costoVentas = ventasGlobal.multiply(BigDecimal.valueOf(0.65));
            BigDecimal margenBruto = ventasGlobal.subtract(costoVentas);
            BigDecimal margenPorcentaje = ventasGlobal.compareTo(BigDecimal.ZERO) > 0
                    ? margenBruto.multiply(BigDecimal.valueOf(100)).divide(ventasGlobal, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // Gastos totales
            BigDecimal gastosGlobal = BigDecimal.ZERO;
            for (ExecutiveDashboardResponse dash : dashboards.values()) {
                if (dash == null) continue;
                ExecutiveDashboardResponse.FinancialKPI fin = dash.getFinancial();
                if (fin != null && "OK".equals(fin.getStatus())) {
                    gastosGlobal = gastosGlobal.add(fin.getExpenses() != null ? fin.getExpenses() : BigDecimal.ZERO);
                }
            }
            BigDecimal rentabilidadNeta = margenBruto.subtract(gastosGlobal);

            ConsolidatedReportResponse.TotalesGlobales totales = ConsolidatedReportResponse.TotalesGlobales.builder()
                    .ventas(ventasGlobal)
                    .costoVentas(costoVentas)
                    .margenBruto(margenBruto)
                    .margenPorcentaje(margenPorcentaje)
                    .gastos(gastosGlobal)
                    .rentabilidadNeta(rentabilidadNeta)
                    .ordenesProduccion(ordenesProduccion)
                    .sucursalesActivas(activeBranches.size())
                    .build();

            // Por sucursal
            List<ConsolidatedReportResponse.BranchSummary> porSucursal = new ArrayList<>();
            for (Branch branch : activeBranches) {
                ExecutiveDashboardResponse dash = dashboards.get(branch.getId());
                if (dash == null) continue;

                ExecutiveDashboardResponse.SalesKPI sales = dash.getSales();
                BigDecimal ventasSuc = sales != null && "OK".equals(sales.getStatus())
                        ? (sales.getTotalSales() != null ? sales.getTotalSales() : BigDecimal.ZERO)
                        : BigDecimal.ZERO;
                double participacion = ventasGlobal.compareTo(BigDecimal.ZERO) > 0
                        ? ventasSuc.multiply(BigDecimal.valueOf(100)).divide(ventasGlobal, 2, RoundingMode.HALF_UP).doubleValue()
                        : 0.0;

                ExecutiveDashboardResponse.FinancialKPI fin = dash.getFinancial();
                ExecutiveDashboardResponse.HRKPI hr = dash.getHr();

                porSucursal.add(ConsolidatedReportResponse.BranchSummary.builder()
                        .branchId(branch.getId())
                        .branchName(branch.getNombre())
                        .ventas(ventasSuc)
                        .ticketPromedio(sales != null ? sales.getAverageTicket() : BigDecimal.ZERO)
                        .margenBruto(fin != null && fin.getIncome() != null ? fin.getIncome() : BigDecimal.ZERO)
                        .participacionPorcentaje(participacion)
                        .transacciones(sales != null ? sales.getTransactionCount() : 0)
                        .ausentismo(hr != null ? hr.getAbsenteeismRate() : BigDecimal.ZERO)
                        .build());
            }

            // Transferencias
            ConsolidatedReportResponse.ResumenTransferencias transferencias = getResumenTransferencias(desde, hasta);

            return ConsolidatedReportResponse.builder()
                    .totales(totales)
                    .porSucursal(porSucursal)
                    .productosCriticos(List.of())
                    .transferencias(transferencias)
                    .status("OK")
                    .build();

        } catch (Exception e) {
            log.error("Error generando reporte consolidado", e);
            return ConsolidatedReportResponse.builder()
                    .status("ERROR")
                    .build();
        }
    }

    private ConsolidatedReportResponse.ResumenTransferencias getResumenTransferencias(LocalDate desde, LocalDate hasta) {
        LocalDateTime start = desde != null ? desde.atStartOfDay() : LocalDateTime.now().minusMonths(1);
        LocalDateTime end = hasta != null ? hasta.atTime(LocalTime.MAX) : LocalDateTime.now();

        List<StockTransfer> transfers = stockTransferRepository
                .findByFechaSolicitudBetweenOrderByFechaSolicitudDesc(start, end);

        long total = transfers.size();
        BigDecimal montoTotal = BigDecimal.valueOf(total * 1000);

        Map<Long, Long> enviosPorSucursal = transfers.stream()
                .collect(Collectors.groupingBy(StockTransfer::getSucursalOrigenId, Collectors.counting()));
        Map<Long, Long> recibosPorSucursal = transfers.stream()
                .filter(t -> t.getEstado() == StockTransfer.Estado.RECIBIDA)
                .collect(Collectors.groupingBy(StockTransfer::getSucursalDestinoId, Collectors.counting()));

        Long masEnvia = enviosPorSucursal.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        Long masRecibe = recibosPorSucursal.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        return ConsolidatedReportResponse.ResumenTransferencias.builder()
                .totalTransferencias(total)
                .montoTotalTransferido(montoTotal)
                .sucursalMasEnvia(masEnvia)
                .sucursalMasRecibe(masRecibe)
                .build();
    }
}
