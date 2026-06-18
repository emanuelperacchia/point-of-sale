package com.pos.system.service;

import com.pos.system.dto.response.ProductAnalysisResponse;
import com.pos.system.dto.response.ProductAnalysisResponse.*;
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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Análisis de productos con clasificación ABC/Pareto (US-037).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductAnalysisService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;

    /**
     * Analiza productos con clasificación ABC y detecta sin movimiento.
     */
    public ProductAnalysisResponse analyzeProducts(LocalDate hasta, int meses, int diasSinVenta) {
        try {
            LocalDate desde = hasta.minusMonths(meses);
            LocalDateTime desdeDt = desde.atStartOfDay();
            LocalDateTime hastaDt = hasta.atTime(LocalTime.MAX);

            // 1. Clasificación ABC
            List<Object[]> raw = saleRepository.findAllProductSales(desdeDt, hastaDt);
            List<ProductoABC> clasificados = clasificarABC(raw);
            ResumenABC resumen = buildResumenABC(clasificados);

            // 2. Productos sin movimiento
            LocalDate limiteSinVenta = hasta.minusDays(diasSinVenta);
            List<Object[]> rawSinMov = saleRepository.findProductsWithoutSalesSince(limiteSinVenta.atStartOfDay());
            // También incluir productos que nunca se vendieron: activos sin ninguna venta
            long totalActivos = productRepository.countByActiveTrue();
            List<ProductoSinMovimiento> sinMovimiento = buildSinMovimiento(rawSinMov, totalActivos, clasificados.size());

            return ProductAnalysisResponse.builder()
                    .clasificacionABC(clasificados)
                    .resumen(resumen)
                    .sinMovimiento(sinMovimiento)
                    .status("OK")
                    .build();
        } catch (Exception e) {
            log.warn("Error en análisis de productos", e);
            return ProductAnalysisResponse.builder().status("ERROR").build();
        }
    }

    private List<ProductoABC> clasificarABC(List<Object[]> raw) {
        if (raw.isEmpty()) return List.of();

        BigDecimal totalVentas = raw.stream()
                .map(r -> (BigDecimal) r[4])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalVentas.compareTo(BigDecimal.ZERO) == 0) return List.of();

        BigDecimal acumulado = BigDecimal.ZERO;
        List<ProductoABC> result = new ArrayList<>();

        for (Object[] row : raw) {
            BigDecimal ventas = (BigDecimal) row[4];
            acumulado = acumulado.add(ventas);
            BigDecimal pctAcumulado = acumulado.multiply(BigDecimal.valueOf(100))
                    .divide(totalVentas, 1, RoundingMode.HALF_UP);

            String clasif;
            // A: 0-80%, B: 80-95%, C: 95-100%
            if (pctAcumulado.compareTo(BigDecimal.valueOf(80)) <= 0) {
                clasif = "A";
            } else if (pctAcumulado.compareTo(BigDecimal.valueOf(95)) <= 0) {
                clasif = "B";
            } else {
                clasif = "C";
            }

            result.add(ProductoABC.builder()
                    .productId(((Number) row[0]).longValue())
                    .productName((String) row[1])
                    .productSku((String) row[2])
                    .categoria((String) row[3])
                    .ventas(ventas)
                    .cantidad(((Number) row[5]).longValue())
                    .porcentajeAcumulado(pctAcumulado)
                    .clasificacion(clasif)
                    .build());
        }

        return result;
    }

    private ResumenABC buildResumenABC(List<ProductoABC> items) {
        long total = items.size();
        long aCount = items.stream().filter(p -> "A".equals(p.getClasificacion())).count();
        long bCount = items.stream().filter(p -> "B".equals(p.getClasificacion())).count();
        long cCount = items.stream().filter(p -> "C".equals(p.getClasificacion())).count();

        BigDecimal ventasA = items.stream().filter(p -> "A".equals(p.getClasificacion()))
                .map(ProductoABC::getVentas).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ventasB = items.stream().filter(p -> "B".equals(p.getClasificacion()))
                .map(ProductoABC::getVentas).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ventasC = items.stream().filter(p -> "C".equals(p.getClasificacion()))
                .map(ProductoABC::getVentas).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVentas = ventasA.add(ventasB).add(ventasC);
        BigDecimal pctA = totalVentas.compareTo(BigDecimal.ZERO) > 0
                ? ventasA.multiply(BigDecimal.valueOf(100)).divide(totalVentas, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return ResumenABC.builder()
                .totalProductos(total)
                .productosClaseA(aCount)
                .productosClaseB(bCount)
                .productosClaseC(cCount)
                .ventasClaseA(ventasA)
                .ventasClaseB(ventasB)
                .ventasClaseC(ventasC)
                .porcentajeACobertura(pctA)
                .build();
    }

    private List<ProductoSinMovimiento> buildSinMovimiento(List<Object[]> rawSinMov,
                                                            long totalActivos, long productosConVentas) {
        List<ProductoSinMovimiento> result = new ArrayList<>();

        // Productos activos sin ventas en período
        for (Object[] row : rawSinMov) {
            result.add(ProductoSinMovimiento.builder()
                    .productId(((Number) row[0]).longValue())
                    .productName((String) row[1])
                    .productSku((String) row[2])
                    .stockActual(0)
                    .costoPromedio(BigDecimal.ZERO)
                    .diasSinVenta(0L)
                    .build());
        }

        // También agregar productos que nunca se vendieron: totalActivos - productosConVentas - rawSinMov.size()
        long nuncaVendidos = totalActivos - productosConVentas - result.size();
        // No creamos items individuales para estos, el frontend puede calcularlo

        return result;
    }
}
