package com.pos.system.service;

import com.pos.system.dto.response.CashFlowDayRow;
import com.pos.system.dto.response.CashFlowResponse;
import com.pos.system.repository.ExpenseRepository;
import com.pos.system.repository.SaleRepository;
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

/**
 * Servicio de flujo de caja: datos reales y proyección.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CashFlowService {

    private final SaleRepository saleRepository;
    private final ExpenseRepository expenseRepository;

    /**
     * Obtiene el flujo de caja para un período, con proyección opcional.
     */
    @Transactional(readOnly = true)
    public CashFlowResponse getCashFlow(LocalDate desde, LocalDate hasta,
                                         boolean incluirProyeccion, int diasProyeccion) {

        // 1. Flujo real: ventas COMPLETADA como ingresos, gastos PAGADO como egresos
        Map<LocalDate, BigDecimal> ingresos = getIngresosReales(desde, hasta);
        Map<LocalDate, BigDecimal> egresos = getEgresosReales(desde, hasta);

        // 2. Construir días del período real
        List<CashFlowDayRow> dias = new ArrayList<>();
        BigDecimal saldoAcumulado = BigDecimal.ZERO;

        for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
            BigDecimal ing = ingresos.getOrDefault(fecha, BigDecimal.ZERO);
            BigDecimal egr = egresos.getOrDefault(fecha, BigDecimal.ZERO);
            saldoAcumulado = saldoAcumulado.add(ing).subtract(egr);
            dias.add(CashFlowDayRow.real(fecha, ing, egr, saldoAcumulado));
        }

        // 3. Proyección
        LocalDate primerNegativo = null;

        if (incluirProyeccion && diasProyeccion > 0) {
            // Promedio diario de ingresos y egresos últimos 90 días
            LocalDate inicioPromedio = desde.minusDays(90);

            Map<LocalDate, BigDecimal> ingresosHist = getIngresosReales(inicioPromedio, desde.minusDays(1));
            Map<LocalDate, BigDecimal> egresosHist = getEgresosReales(inicioPromedio, desde.minusDays(1));

            long diasHist = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(inicioPromedio, desde));
            BigDecimal promedioIngresoDiario = ingresosHist.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(diasHist), 2, RoundingMode.HALF_UP);
            BigDecimal promedioEgresoDiario = egresosHist.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(diasHist), 2, RoundingMode.HALF_UP);

            // Gastos recurrentes futuros conocidos
            List<Object[]> futuros = expenseRepository.findDailyExpenseTotals(
                    hasta.plusDays(1), hasta.plusDays(diasProyeccion + 1));

            for (int i = 1; i <= diasProyeccion; i++) {
                LocalDate fecha = hasta.plusDays(i);
                BigDecimal ing = promedioIngresoDiario;
                BigDecimal egr = promedioEgresoDiario;

                // Si hay gastos recurrentes conocidos, usarlos
                for (Object[] row : futuros) {
                    LocalDate fechaGasto = ((java.sql.Date) row[0]).toLocalDate();
                    if (fechaGasto.equals(fecha)) {
                        egr = (BigDecimal) row[1];
                        break;
                    }
                }

                saldoAcumulado = saldoAcumulado.add(ing).subtract(egr);
                dias.add(CashFlowDayRow.proyectado(fecha, ing, egr, saldoAcumulado));

                if (primerNegativo == null && saldoAcumulado.compareTo(BigDecimal.ZERO) < 0) {
                    primerNegativo = fecha;
                }
            }
        }

        if (primerNegativo != null) {
            return CashFlowResponse.conAlerta(dias, primerNegativo);
        }
        return CashFlowResponse.sinAlerta(dias);
    }

    private Map<LocalDate, BigDecimal> getIngresosReales(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || desde.isAfter(hasta)) return Collections.emptyMap();

        Map<LocalDate, BigDecimal> result = new LinkedHashMap<>();
        LocalDateTime desdeDt = desde.atStartOfDay();
        LocalDateTime hastaDt = hasta.plusDays(1).atStartOfDay();

        List<Object[]> raw = saleRepository.findDailySalesTotals(desdeDt, hastaDt);
        for (Object[] row : raw) {
            LocalDate fecha = ((java.sql.Date) row[0]).toLocalDate();
            BigDecimal total = (BigDecimal) row[1];
            result.put(fecha, total);
        }
        return result;
    }

    private Map<LocalDate, BigDecimal> getEgresosReales(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || desde.isAfter(hasta)) return Collections.emptyMap();

        Map<LocalDate, BigDecimal> result = new LinkedHashMap<>();
        List<Object[]> raw = expenseRepository.findDailyExpenseTotals(desde, hasta.plusDays(1));
        for (Object[] row : raw) {
            LocalDate fecha = ((java.sql.Date) row[0]).toLocalDate();
            BigDecimal total = (BigDecimal) row[1];
            result.put(fecha, total);
        }
        return result;
    }
}
