package com.pos.system.service;

import com.pos.system.dto.response.AttendanceSummaryResponse;
import com.pos.system.dto.response.CommissionResultResponse;
import com.pos.system.entity.Employee;
import com.pos.system.entity.Payroll;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Orquesta AttendanceService, CommissionService, EvaluationService y
 * ConfigurationService para calcular todos los campos de un recibo de sueldo.
 * NO persiste — devuelve un Payroll en estado BORRADOR para que PayrollService
 * decida si guardarlo o no.
 */
@Service
@RequiredArgsConstructor
public class PayrollCalculatorService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceService attendanceService;
    private final CommissionService commissionService;
    private final ConfigurationService configurationService;

    // Default discount percentages (used if not found in system_configurations)
    private static final BigDecimal DEFAULT_DESC_JUBILACION = new BigDecimal("11.00");
    private static final BigDecimal DEFAULT_DESC_OBRA_SOCIAL = new BigDecimal("3.00");
    private static final BigDecimal DEFAULT_DESC_ANSES       = new BigDecimal("3.00");

    public Payroll calculate(Long employeeId, int mes, int anio) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado"));

        if (employee.getUserId() == null) {
            throw new BadRequestException("El empleado no tiene un usuario vinculado");
        }

        // ── Attendance ──────────────────────────────────────────────
        AttendanceSummaryResponse attendance = attendanceService.getSummary(employeeId, mes, anio);

        int diasTrabajados = attendance.getDiasTrabajados();
        int minutosNormales = attendance.getHorasTotalesMinutos() - attendance.getHorasExtraMinutos();
        int minutosExtra = attendance.getHorasExtraMinutos();

        // ── Salary (prorated by days worked) ────────────────────────
        int diasDelMes = LocalDate.of(anio, mes, 1).lengthOfMonth();
        if (diasTrabajados == 0) diasTrabajados = diasDelMes; // fallback: mes completo

        BigDecimal sueldoBasico = employee.getSalarioBase()
                .multiply(BigDecimal.valueOf(diasTrabajados))
                .divide(BigDecimal.valueOf(diasDelMes), 2, RoundingMode.HALF_UP);

        // ── Horas extra (50% recargo) ──────────────────────────────
        int horasPorDia = 8;
        BigDecimal valorHoraBase = employee.getSalarioBase()
                .divide(BigDecimal.valueOf(diasDelMes * horasPorDia), 2, RoundingMode.HALF_UP);
        BigDecimal plusHorasExtra = valorHoraBase
                .multiply(BigDecimal.valueOf(minutosExtra))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("0.50"))
                .setScale(2, RoundingMode.HALF_UP);

        // ── Comisiones ──────────────────────────────────────────────
        CommissionResultResponse comision = commissionService.getSummary(employeeId, mes, anio);
        BigDecimal comisiones = comision != null ? comision.getComisionCalculada() : BigDecimal.ZERO;

        // ── Bono desempeño (si hay evaluación finalizada) ───────────
        // Por ahora se deja en 0; podría integrarse con EvaluationService
        BigDecimal bonoDesempeno = BigDecimal.ZERO;

        // ── Total haberes ───────────────────────────────────────────
        BigDecimal totalHaberes = sueldoBasico
                .add(plusHorasExtra)
                .add(comisiones)
                .add(bonoDesempeno);

        // ── Descuentos legales ──────────────────────────────────────
        BigDecimal pctJubilacion  = getPctDescuento("descuento.jubilacion.porcentaje",  DEFAULT_DESC_JUBILACION);
        BigDecimal pctObraSocial  = getPctDescuento("descuento.obra_social.porcentaje", DEFAULT_DESC_OBRA_SOCIAL);
        BigDecimal pctAnses       = getPctDescuento("descuento.anses.porcentaje",       DEFAULT_DESC_ANSES);

        BigDecimal descJubilacion  = totalHaberes.multiply(pctJubilacion).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal descObraSocial  = totalHaberes.multiply(pctObraSocial).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal descAnses       = totalHaberes.multiply(pctAnses).divide(BigDecimal.valueOf(100),      2, RoundingMode.HALF_UP);

        // ── Descuentos por ausencias injustificadas ─────────────────
        int ausenciasInjustificadas = attendance.getAusenciasInjustificadas();
        BigDecimal valorDia = employee.getSalarioBase()
                .divide(BigDecimal.valueOf(diasDelMes), 2, RoundingMode.HALF_UP);
        BigDecimal descAusencias = valorDia.multiply(BigDecimal.valueOf(ausenciasInjustificadas));

        // ── Embargos (sin implementar — se agregan via PayrollAdjustment) ──
        BigDecimal descEmbargos = BigDecimal.ZERO;

        BigDecimal totalDescuentos = descJubilacion
                .add(descObraSocial)
                .add(descAnses)
                .add(descAusencias)
                .add(descEmbargos);

        BigDecimal netoApagar = totalHaberes.subtract(totalDescuentos);
        if (netoApagar.compareTo(BigDecimal.ZERO) < 0) netoApagar = BigDecimal.ZERO;

        return Payroll.builder()
                .employeeId(employeeId)
                .mes(mes)
                .anio(anio)
                .diasTrabajados(diasTrabajados)
                .horasNormalesMinutos(minutosNormales)
                .horasExtraMinutos(minutosExtra)
                .sueldoBasico(sueldoBasico)
                .plusHorasExtra(plusHorasExtra)
                .comisiones(comisiones)
                .bonoDesempeno(bonoDesempeno)
                .totalHaberes(totalHaberes)
                .descJubilacion(descJubilacion)
                .descObraSocial(descObraSocial)
                .descAnses(descAnses)
                .descAusencias(descAusencias)
                .descEmbargos(descEmbargos)
                .totalDescuentos(totalDescuentos)
                .netoApagar(netoApagar)
                .estado(Payroll.Estado.BORRADOR)
                .build();
    }

    private BigDecimal getPctDescuento(String key, BigDecimal defaultValue) {
        try {
            return BigDecimal.valueOf(configurationService.getDoubleConfig(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
