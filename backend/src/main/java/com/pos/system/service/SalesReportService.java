package com.pos.system.service;

import com.pos.system.dto.response.SalesReportResponse;
import com.pos.system.dto.response.SalesReportResponse.*;
import com.pos.system.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Reporte avanzado de ventas con métricas desglosadas y comparativas (US-036).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesReportService {

    private static final String[] DIAS = {"Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"};

    private final SaleRepository saleRepository;

    /**
     * Genera reporte avanzado de ventas para el período.
     */
    public SalesReportResponse advancedReport(LocalDate desde, LocalDate hasta) {
        LocalDateTime desdeDt = desde.atStartOfDay();
        LocalDateTime hastaDt = hasta.atTime(LocalTime.MAX);
        long diasPeriodo = ChronoUnit.DAYS.between(desde, hasta) + 1;

        // Periodo anterior de igual duración
        LocalDate antDesde = desde.minusDays(diasPeriodo);
        LocalDate antHasta = desde.minusDays(1);
        LocalDateTime antDesdeDt = antDesde.atStartOfDay();
        LocalDateTime antHastaDt = antHasta.atTime(LocalTime.MAX);

        return SalesReportResponse.builder()
                .resumen(buildResumen(desdeDt, hastaDt, antDesdeDt, antHastaDt))
                .porMetodoPago(buildSalesByPaymentMethod(desdeDt, hastaDt))
                .ventasPorHora(buildSalesByHour(desdeDt, hastaDt))
                .ventasPorDiaSemana(buildSalesByDayOfWeek(desdeDt, hastaDt))
                .comparativa(buildComparativa(desdeDt, hastaDt, antDesdeDt, antHastaDt, diasPeriodo))
                .periodo(desde + " — " + hasta)
                .periodoAnterior(antDesde + " — " + antHasta)
                .build();
    }

    private Resumen buildResumen(LocalDateTime desde, LocalDateTime hasta,
                                  LocalDateTime antDesde, LocalDateTime antHasta) {
        try {
            BigDecimal total = saleRepository.sumTotalByCreatedAtBetween(desde, hasta);
            long transacciones = saleRepository.countByStatusAndCreatedAtBetween(
                    com.pos.system.entity.SaleStatus.COMPLETED, desde, hasta);
            BigDecimal totalAnt = saleRepository.sumTotalByCreatedAtBetween(antDesde, antHasta);
            BigDecimal devoluciones = saleRepository.sumRefundsByCreatedAtBetween(desde, hasta);
            BigDecimal descuentos = saleRepository.sumDiscountsByCreatedAtBetween(desde, hasta);
            BigDecimal impuestos = saleRepository.sumTaxesByCreatedAtBetween(desde, hasta);

            BigDecimal ticketPromedio = transacciones > 0
                    ? total.divide(BigDecimal.valueOf(transacciones), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal variacion = calcularVariacion(total, totalAnt);

            return Resumen.builder()
                    .totalVentas(total)
                    .cantidadTransacciones(transacciones)
                    .ticketPromedio(ticketPromedio)
                    .totalDevoluciones(devoluciones)
                    .descuentosAplicados(descuentos)
                    .impuestosCobrados(impuestos)
                    .variacionVsPeriodoAnterior(variacion)
                    .status("OK")
                    .build();
        } catch (Exception e) {
            log.warn("Error construyendo resumen ventas", e);
            return Resumen.builder().status("ERROR").build();
        }
    }

    private List<SalesByPaymentMethod> buildSalesByPaymentMethod(LocalDateTime desde, LocalDateTime hasta) {
        try {
            List<Object[]> raw = saleRepository.findSalesByPaymentMethod(desde, hasta);
            BigDecimal total = raw.stream()
                    .map(r -> (BigDecimal) r[1])
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (total.compareTo(BigDecimal.ZERO) == 0) return List.of();

            return raw.stream()
                    .map(r -> SalesByPaymentMethod.builder()
                            .metodo((String) r[0])
                            .monto((BigDecimal) r[1])
                            .cantidad(((Number) r[2]).longValue())
                            .porcentaje(((BigDecimal) r[1]).multiply(BigDecimal.valueOf(100))
                                    .divide(total, 1, RoundingMode.HALF_UP))
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Error en ventas por método de pago", e);
            return List.of();
        }
    }

    private List<SalesByHour> buildSalesByHour(LocalDateTime desde, LocalDateTime hasta) {
        try {
            List<Object[]> raw = saleRepository.findSalesByHour(desde, hasta);
            return raw.stream()
                    .map(r -> SalesByHour.builder()
                            .hora(((Number) r[0]).intValue())
                            .monto((BigDecimal) r[1])
                            .cantidad(((Number) r[2]).longValue())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Error en ventas por hora", e);
            return List.of();
        }
    }

    private List<SalesByDayOfWeek> buildSalesByDayOfWeek(LocalDateTime desde, LocalDateTime hasta) {
        try {
            List<Object[]> raw = saleRepository.findSalesByDayOfWeek(desde, hasta);
            return raw.stream()
                    .map(r -> {
                        int diaNum = ((Number) r[0]).intValue();
                        // PostgreSQL: 0=Sunday, 1=Monday, ..., 6=Saturday
                        String diaNombre = diaNum >= 0 && diaNum < 7 ? DIAS[diaNum] : "?";
                        return SalesByDayOfWeek.builder()
                                .dia(diaNombre)
                                .diaNumero(diaNum)
                                .monto((BigDecimal) r[1])
                                .cantidad(((Number) r[2]).longValue())
                                .build();
                    })
                    .sorted(Comparator.comparingInt(SalesByDayOfWeek::getDiaNumero))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Error en ventas por día de semana", e);
            return List.of();
        }
    }

    private List<ComparativaPeriodo> buildComparativa(LocalDateTime desde, LocalDateTime hasta,
                                                       LocalDateTime antDesde, LocalDateTime antHasta,
                                                       long diasPeriodo) {
        try {
            Map<LocalDate, BigDecimal> actualMap = toDailyMap(
                    saleRepository.findDailySalesTotals(desde, hasta));
            Map<LocalDate, BigDecimal> anteriorMap = toDailyMap(
                    saleRepository.findDailySalesTotals(antDesde, antHasta));

            List<ComparativaPeriodo> result = new ArrayList<>();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");

            // Generar lista completa de días
            for (int i = 0; i < diasPeriodo; i++) {
                LocalDate fechaActual = desde.toLocalDate().plusDays(i);
                LocalDate fechaAnt = antDesde.toLocalDate().plusDays(i);

                result.add(ComparativaPeriodo.builder()
                        .fecha(fechaActual.format(fmt))
                        .montoActual(actualMap.getOrDefault(fechaActual, BigDecimal.ZERO))
                        .montoAnterior(anteriorMap.getOrDefault(fechaAnt, BigDecimal.ZERO))
                        .build());
            }

            return result;
        } catch (Exception e) {
            log.warn("Error en comparativa", e);
            return List.of();
        }
    }

    private Map<LocalDate, BigDecimal> toDailyMap(List<Object[]> raw) {
        Map<LocalDate, BigDecimal> map = new LinkedHashMap<>();
        for (Object[] row : raw) {
            LocalDate fecha = ((java.sql.Date) row[0]).toLocalDate();
            map.put(fecha, (BigDecimal) row[1]);
        }
        return map;
    }

    private BigDecimal calcularVariacion(BigDecimal actual, BigDecimal anterior) {
        if (anterior.compareTo(BigDecimal.ZERO) == 0) {
            return actual.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return actual.subtract(anterior)
                .multiply(BigDecimal.valueOf(100))
                .divide(anterior, 2, RoundingMode.HALF_UP);
    }
}
