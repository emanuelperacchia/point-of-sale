package com.pos.system.service;

import com.pos.system.dto.response.HRReportResponse;
import com.pos.system.dto.response.HRReportResponse.*;
import com.pos.system.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Reportes de RRHH con ausentismo y productividad (US-040).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HRReportService {

    private final EmployeeRepository employeeRepository;
    private final PayrollRepository payrollRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final SaleRepository saleRepository;

    public HRReportResponse getHRReport(Integer mes, Integer anio) {
        try {
            if (mes == null) mes = LocalDate.now().getMonthValue();
            if (anio == null) anio = LocalDate.now().getYear();

            ResumenRRHH resumen = buildResumen(mes, anio);
            List<ProductividadVendedor> productividad = buildProductividad(mes, anio);

            return HRReportResponse.builder()
                    .resumen(resumen)
                    .productividad(productividad)
                    .status("OK")
                    .build();
        } catch (Exception e) {
            log.warn("Error en reporte RRHH", e);
            return HRReportResponse.builder().status("ERROR").build();
        }
    }

    private ResumenRRHH buildResumen(Integer mes, Integer anio) {
        List<com.pos.system.entity.Employee> activos = employeeRepository.findByActivoTrue();
        long empleadosActivos = activos.size();

        BigDecimal costoLaboral = payrollRepository.sumNetoByMesAndAnio(mes, anio);
        BigDecimal salarioPromedio = empleadosActivos > 0 && costoLaboral != null
                ? costoLaboral.divide(BigDecimal.valueOf(empleadosActivos), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        LocalDate inicioMes = LocalDate.of(anio, mes, 1);
        LocalDate finMes = inicioMes.withDayOfMonth(inicioMes.lengthOfMonth());

        long ausencias = attendanceRecordRepository.countAusenciasByPeriod(inicioMes, finMes);
        long totalAusenciasEsperadas = empleadosActivos * finMes.lengthOfMonth();
        double ausentismoPct = totalAusenciasEsperadas > 0
                ? (double) ausencias / totalAusenciasEsperadas * 100
                : 0.0;

        return ResumenRRHH.builder()
                .empleadosActivos(empleadosActivos)
                .costoLaboralMensual(costoLaboral != null ? costoLaboral : BigDecimal.ZERO)
                .salarioPromedio(salarioPromedio)
                .ausentismoPct(Math.round(ausentismoPct * 100.0) / 100.0)
                .ausenciasMes(ausencias)
                .totalEmpleadosEsperados(totalAusenciasEsperadas)
                .build();
    }

    private List<ProductividadVendedor> buildProductividad(Integer mes, Integer anio) {
        // Consulta a SaleRepository para ventas por vendedor en el mes
        LocalDate inicioMes = LocalDate.of(anio, mes, 1);
        LocalDate finMes = inicioMes.withDayOfMonth(inicioMes.lengthOfMonth());
        var desde = inicioMes.atStartOfDay();
        var hasta = finMes.plusDays(1).atStartOfDay();

        // Usar findTopSellers sin límite para obtener todos
        List<Object[]> raw = saleRepository.findTopSellers(desde, hasta);

        List<ProductividadVendedor> result = new ArrayList<>();
        for (Object[] row : raw) {
            BigDecimal ventas = (BigDecimal) row[2];
            Long transacciones = ((Number) row[3]).longValue();

            // Horas trabajadas en el período (estimado 160h/mes)
            BigDecimal ventasPorHora = BigDecimal.valueOf(160).compareTo(BigDecimal.ZERO) > 0
                    ? ventas.divide(BigDecimal.valueOf(160), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            result.add(ProductividadVendedor.builder()
                    .nombre((String) row[1])
                    .ventasPeriodo(ventas)
                    .transacciones(transacciones)
                    .ventasPorHora(ventasPorHora)
                    .comisiones(BigDecimal.ZERO)
                    .build());
        }
        return result;
    }
}
