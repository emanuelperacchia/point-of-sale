package com.pos.system.service;

import com.pos.system.dto.response.PayrollAdjustmentResponse;
import com.pos.system.dto.response.PayrollResponse;
import com.pos.system.entity.Payroll;
import com.pos.system.entity.PayrollAdjustment;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.PayrollAdjustmentRepository;
import com.pos.system.repository.PayrollRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock
    private PayrollRepository payrollRepository;

    @Mock
    private PayrollAdjustmentRepository adjustmentRepository;

    @Mock
    private PayrollCalculatorService calculatorService;

    @InjectMocks
    private PayrollService payrollService;

    private static final Long EMPLOYEE_ID = 1L;
    private static final int MES = 6;
    private static final int ANIO = 2026;

    private Payroll buildPayroll(Long id) {
        return Payroll.builder()
                .id(id)
                .employeeId(EMPLOYEE_ID)
                .mes(MES)
                .anio(ANIO)
                .diasTrabajados(30)
                .sueldoBasico(new BigDecimal("500000"))
                .netoApagar(new BigDecimal("400000"))
                .estado(Payroll.Estado.BORRADOR)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void calcularYGuardar_ShouldCalculateAndSave() {
        given(payrollRepository.findByEmployeeIdAndMesAndAnio(EMPLOYEE_ID, MES, ANIO))
                .willReturn(Optional.empty());

        Payroll calculated = buildPayroll(null);
        given(calculatorService.calculate(EMPLOYEE_ID, MES, ANIO))
                .willReturn(calculated);

        Payroll saved = buildPayroll(1L);
        given(payrollRepository.save(calculated))
                .willReturn(saved);

        PayrollResponse response = payrollService.calcularYGuardar(EMPLOYEE_ID, MES, ANIO);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
        assertThat(response.getMes()).isEqualTo(MES);
        assertThat(response.getAnio()).isEqualTo(ANIO);
        assertThat(response.getDiasTrabajados()).isEqualTo(30);
        assertThat(response.getSueldoBasico()).isEqualByComparingTo("500000");
        assertThat(response.getNetoApagar()).isEqualByComparingTo("400000");
        assertThat(response.getEstado()).isEqualTo(Payroll.Estado.BORRADOR);
    }

    @Test
    void calcularYGuardar_WhenAlreadyExists_ShouldThrow() {
        given(payrollRepository.findByEmployeeIdAndMesAndAnio(EMPLOYEE_ID, MES, ANIO))
                .willReturn(Optional.of(buildPayroll(1L)));

        assertThatThrownBy(() -> payrollService.calcularYGuardar(EMPLOYEE_ID, MES, ANIO))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Ya existe una liquidación");

        then(calculatorService).shouldHaveNoInteractions();
    }

    @Test
    void obtenerPorId_ShouldReturnResponse() {
        given(payrollRepository.findById(1L))
                .willReturn(Optional.of(buildPayroll(1L)));

        PayrollResponse response = payrollService.obtenerPorId(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
    }

    @Test
    void obtenerPorId_WhenNotFound_ShouldThrow() {
        given(payrollRepository.findById(99L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> payrollService.obtenerPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Liquidación no encontrada");
    }

    @Test
    void aprobar_ShouldApprovePayroll() {
        Payroll payroll = buildPayroll(1L);
        given(payrollRepository.findById(1L))
                .willReturn(Optional.of(payroll));

        Payroll approved = buildPayroll(1L);
        approved.setEstado(Payroll.Estado.APROBADA);
        approved.setAprobadoPor(999L);
        approved.setFechaAprobacion(LocalDate.now());
        given(payrollRepository.save(payroll))
                .willReturn(approved);

        PayrollResponse response = payrollService.aprobar(1L, 999L);

        assertThat(response.getEstado()).isEqualTo(Payroll.Estado.APROBADA);
        assertThat(response.getAprobadoPor()).isEqualTo(999L);
        assertThat(response.getFechaAprobacion()).isEqualTo(LocalDate.now());
    }

    @Test
    void aprobar_WhenAlreadyApproved_ShouldThrow() {
        Payroll payroll = buildPayroll(1L);
        payroll.setEstado(Payroll.Estado.APROBADA);
        given(payrollRepository.findById(1L))
                .willReturn(Optional.of(payroll));

        assertThatThrownBy(() -> payrollService.aprobar(1L, 999L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ya está aprobada");

        then(payrollRepository).shouldHaveNoMoreInteractions();
        then(payrollRepository).should(never()).save(payroll);
    }

    @Test
    void agregarAjuste_ShouldSaveAdjustment() {
        given(payrollRepository.existsById(1L)).willReturn(true);

        PayrollAdjustment adjustment = PayrollAdjustment.builder()
                .payrollId(1L)
                .concepto("Bono")
                .monto(new BigDecimal("50000"))
                .justificacion("Desempeño excepcional")
                .creadoPor(EMPLOYEE_ID)
                .build();

        PayrollAdjustment saved = PayrollAdjustment.builder()
                .id(10L)
                .payrollId(1L)
                .concepto("Bono")
                .monto(new BigDecimal("50000"))
                .justificacion("Desempeño excepcional")
                .creadoPor(EMPLOYEE_ID)
                .createdAt(LocalDateTime.now())
                .build();

        given(adjustmentRepository.save(adjustment)).willReturn(saved);

        PayrollAdjustmentResponse response = payrollService.agregarAjuste(
                1L, "Bono", new BigDecimal("50000"),
                "Desempeño excepcional", EMPLOYEE_ID);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getPayrollId()).isEqualTo(1L);
        assertThat(response.getConcepto()).isEqualTo("Bono");
        assertThat(response.getMonto()).isEqualByComparingTo("50000");
        assertThat(response.getJustificacion()).isEqualTo("Desempeño excepcional");
        assertThat(response.getCreadoPor()).isEqualTo(EMPLOYEE_ID);
    }

    @Test
    void agregarAjuste_WhenPayrollNotFound_ShouldThrow() {
        given(payrollRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> payrollService.agregarAjuste(
                99L, "Bono", new BigDecimal("50000"),
                "Test", EMPLOYEE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Liquidación no encontrada");

        then(adjustmentRepository).shouldHaveNoInteractions();
    }

    @Test
    void listarPorEmpleadoYAnio_ShouldReturnList() {
        given(payrollRepository.findByEmployeeIdAndAnio(EMPLOYEE_ID, ANIO))
                .willReturn(List.of(buildPayroll(1L), buildPayroll(2L)));

        List<PayrollResponse> responses = payrollService.listarPorEmpleadoYAnio(EMPLOYEE_ID, ANIO);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo(1L);
        assertThat(responses.get(1).getId()).isEqualTo(2L);
    }

    @Test
    void listarPorPeriodo_ShouldReturnList() {
        given(payrollRepository.findByMesAndAnio(MES, ANIO))
                .willReturn(List.of(buildPayroll(1L), buildPayroll(2L)));

        List<PayrollResponse> responses = payrollService.listarPorPeriodo(MES, ANIO);

        assertThat(responses).hasSize(2);
    }
}
