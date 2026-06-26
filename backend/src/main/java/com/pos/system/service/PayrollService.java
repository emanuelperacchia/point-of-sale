package com.pos.system.service;

import com.pos.system.dto.response.PayrollAdjustmentResponse;
import com.pos.system.dto.response.PayrollResponse;
import com.pos.system.entity.Payroll;
import com.pos.system.entity.PayrollAdjustment;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.PayrollAdjustmentRepository;
import com.pos.system.repository.PayrollRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private final PayrollRepository payrollRepository;
    private final PayrollAdjustmentRepository adjustmentRepository;
    private final PayrollCalculatorService calculatorService;

    @Autowired(required = false)
    private AccountingService accountingService;

    // ── Calculate & Save ────────────────────────────────────────────

    @Transactional
    public PayrollResponse calcularYGuardar(Long employeeId, int mes, int anio) {
        if (payrollRepository.findByEmployeeIdAndMesAndAnio(employeeId, mes, anio).isPresent()) {
            throw new BadRequestException("Ya existe una liquidación para este empleado en el período");
        }
        Payroll payroll = calculatorService.calculate(employeeId, mes, anio);
        payroll = payrollRepository.save(payroll);
        return mapToResponse(payroll);
    }

    @Transactional(readOnly = true)
    public PayrollResponse obtenerPorId(Long id) {
        return payrollRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Liquidación no encontrada"));
    }

    @Transactional(readOnly = true)
    public Payroll obtenerEntity(Long id) {
        return payrollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Liquidación no encontrada"));
    }

    @Transactional(readOnly = true)
    public List<PayrollResponse> listarPorEmpleadoYAnio(Long employeeId, Integer anio) {
        return payrollRepository.findByEmployeeIdAndAnio(employeeId, anio)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PayrollResponse> listarPorPeriodo(Integer mes, Integer anio) {
        return payrollRepository.findByMesAndAnio(mes, anio)
                .stream().map(this::mapToResponse).toList();
    }

    // ── Approval ────────────────────────────────────────────────────

    @Transactional
    public PayrollResponse aprobar(Long id, Long aprobadoPor) {
        Payroll payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Liquidación no encontrada"));
        if (payroll.getEstado() == Payroll.Estado.APROBADA) {
            throw new BadRequestException("La liquidación ya está aprobada");
        }
        payroll.setEstado(Payroll.Estado.APROBADA);
        payroll.setAprobadoPor(aprobadoPor);
        payroll.setFechaAprobacion(LocalDate.now());
        payroll = payrollRepository.save(payroll);

        // Generar asiento contable automático
        if (accountingService != null) {
            BigDecimal cargasSociales = BigDecimal.ZERO;
            if (payroll.getDescJubilacion() != null) cargasSociales = cargasSociales.add(payroll.getDescJubilacion());
            if (payroll.getDescObraSocial() != null) cargasSociales = cargasSociales.add(payroll.getDescObraSocial());
            if (payroll.getDescAnses() != null) cargasSociales = cargasSociales.add(payroll.getDescAnses());

            accountingService.generateEntry("NOMINA", payroll.getId(),
                    "Liquidación #" + payroll.getId() + " - Empleado " + payroll.getEmployeeId(),
                    Map.of(
                            "SUELDOS", payroll.getTotalHaberes() != null ? payroll.getTotalHaberes() : BigDecimal.ZERO,
                            "CARGAS_SOCIALES", cargasSociales,
                            "NETO", payroll.getNetoApagar() != null ? payroll.getNetoApagar() : BigDecimal.ZERO
                    ));
        }

        return mapToResponse(payroll);
    }

    // ── Adjustments ─────────────────────────────────────────────────

    @Transactional
    public PayrollAdjustmentResponse agregarAjuste(Long payrollId, String concepto,
                                                    BigDecimal monto, String justificacion,
                                                    Long creadoPor) {
        if (!payrollRepository.existsById(payrollId)) {
            throw new ResourceNotFoundException("Liquidación no encontrada");
        }
        PayrollAdjustment adjustment = PayrollAdjustment.builder()
                .payrollId(payrollId)
                .concepto(concepto)
                .monto(monto)
                .justificacion(justificacion)
                .creadoPor(creadoPor)
                .build();
        adjustment = adjustmentRepository.save(adjustment);
        return mapToAdjustmentResponse(adjustment);
    }

    @Transactional(readOnly = true)
    public List<PayrollAdjustmentResponse> listarAjustes(Long payrollId) {
        return adjustmentRepository.findByPayrollId(payrollId)
                .stream().map(this::mapToAdjustmentResponse).toList();
    }

    // ── Mappers ─────────────────────────────────────────────────────

    private PayrollResponse mapToResponse(Payroll p) {
        return PayrollResponse.builder()
                .id(p.getId())
                .employeeId(p.getEmployeeId())
                .mes(p.getMes())
                .anio(p.getAnio())
                .diasTrabajados(p.getDiasTrabajados())
                .horasNormalesMinutos(p.getHorasNormalesMinutos())
                .horasExtraMinutos(p.getHorasExtraMinutos())
                .sueldoBasico(p.getSueldoBasico())
                .plusHorasExtra(p.getPlusHorasExtra())
                .comisiones(p.getComisiones())
                .bonoDesempeno(p.getBonoDesempeno())
                .totalHaberes(p.getTotalHaberes())
                .descJubilacion(p.getDescJubilacion())
                .descObraSocial(p.getDescObraSocial())
                .descAnses(p.getDescAnses())
                .descAusencias(p.getDescAusencias())
                .descEmbargos(p.getDescEmbargos())
                .totalDescuentos(p.getTotalDescuentos())
                .netoApagar(p.getNetoApagar())
                .estado(p.getEstado())
                .aprobadoPor(p.getAprobadoPor())
                .fechaAprobacion(p.getFechaAprobacion())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private PayrollAdjustmentResponse mapToAdjustmentResponse(PayrollAdjustment a) {
        return PayrollAdjustmentResponse.builder()
                .id(a.getId())
                .payrollId(a.getPayrollId())
                .concepto(a.getConcepto())
                .monto(a.getMonto())
                .justificacion(a.getJustificacion())
                .creadoPor(a.getCreadoPor())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
