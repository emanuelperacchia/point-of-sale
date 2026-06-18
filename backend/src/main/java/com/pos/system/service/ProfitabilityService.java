package com.pos.system.service;

import com.pos.system.dto.response.ProfitabilityResponse;
import com.pos.system.dto.response.ProfitabilityResponse.*;
import com.pos.system.repository.ExpenseRepository;
import com.pos.system.repository.ProductRepository;
import com.pos.system.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Análisis de rentabilidad con márgenes y punto de equilibrio (US-039).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfitabilityService {

    private final SaleRepository saleRepository;
    private final ExpenseRepository expenseRepository;
    private final ProductRepository productRepository;

    public ProfitabilityResponse analyzeProfitability(LocalDate desde, LocalDate hasta) {
        try {
            LocalDateTime desdeDt = desde.atStartOfDay();
            LocalDateTime hastaDt = hasta.atTime(LocalTime.MAX);

            BigDecimal ingresos = saleRepository.sumTotalByCreatedAtBetween(desdeDt, hastaDt);
            BigDecimal gastos = expenseRepository.sumExpensesByPeriod(desde, hasta);
            BigDecimal costos = BigDecimal.ZERO; // simplificado — costo de ventas

            MargenGeneral mg = buildMargenGeneral(ingresos, costos, gastos);
            List<MargenPorProducto> porProducto = buildMargenPorProducto(desdeDt, hastaDt);
            PuntoEquilibrio pe = buildPuntoEquilibrio(gastos, ingresos);

            return ProfitabilityResponse.builder()
                    .margenGeneral(mg)
                    .porProducto(porProducto)
                    .puntoEquilibrio(pe)
                    .status("OK")
                    .build();
        } catch (Exception e) {
            log.warn("Error en análisis de rentabilidad", e);
            return ProfitabilityResponse.builder().status("ERROR").build();
        }
    }

    private MargenGeneral buildMargenGeneral(BigDecimal ingresos, BigDecimal costos, BigDecimal gastos) {
        BigDecimal gananciaBruta = ingresos.subtract(costos);
        BigDecimal margenBruto = ingresos.compareTo(BigDecimal.ZERO) > 0
                ? gananciaBruta.multiply(BigDecimal.valueOf(100)).divide(ingresos, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal gananciaNeta = ingresos.subtract(gastos);
        BigDecimal margenNeto = ingresos.compareTo(BigDecimal.ZERO) > 0
                ? gananciaNeta.multiply(BigDecimal.valueOf(100)).divide(ingresos, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return MargenGeneral.builder()
                .ingresos(ingresos)
                .costos(costos)
                .gananciaBruta(gananciaBruta)
                .margenBrutoPct(margenBruto)
                .gastosOperativos(gastos)
                .gananciaNeta(gananciaNeta)
                .margenNetoPct(margenNeto)
                .build();
    }

    private List<MargenPorProducto> buildMargenPorProducto(LocalDateTime desde, LocalDateTime hasta) {
        List<Object[]> raw = saleRepository.findAllProductSales(desde, hasta);
        List<MargenPorProducto> result = new ArrayList<>();
        for (Object[] row : raw) {
            BigDecimal ventas = (BigDecimal) row[4];
            BigDecimal costo = BigDecimal.ZERO; // simplificado
            BigDecimal ganancia = ventas.subtract(costo);
            BigDecimal margen = ventas.compareTo(BigDecimal.ZERO) > 0
                    ? ganancia.multiply(BigDecimal.valueOf(100)).divide(ventas, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            result.add(MargenPorProducto.builder()
                    .productId(((Number) row[0]).longValue())
                    .productName((String) row[1])
                    .ventas(ventas)
                    .costo(costo)
                    .ganancia(ganancia)
                    .margenPct(margen)
                    .build());
        }
        return result;
    }

    private PuntoEquilibrio buildPuntoEquilibrio(BigDecimal gastosFijos, BigDecimal ingresos) {
        BigDecimal margenContribucion = BigDecimal.valueOf(40); // 40% estimado
        BigDecimal puntoEq = gastosFijos.compareTo(BigDecimal.ZERO) > 0 && margenContribucion.compareTo(BigDecimal.ZERO) > 0
                ? gastosFijos.multiply(BigDecimal.valueOf(100)).divide(margenContribucion, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return PuntoEquilibrio.builder()
                .gastosFijos(gastosFijos)
                .margenContribucionPct(margenContribucion)
                .puntoEquilibrioMonto(puntoEq)
                .build();
    }
}
