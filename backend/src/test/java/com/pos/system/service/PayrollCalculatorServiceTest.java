package com.pos.system.service;

import com.pos.system.dto.response.AttendanceSummaryResponse;
import com.pos.system.dto.response.CommissionResultResponse;
import com.pos.system.entity.Employee;
import com.pos.system.entity.Payroll;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PayrollCalculatorServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private AttendanceService attendanceService;
    @Mock private CommissionService commissionService;
    @Mock private ConfigurationService configurationService;

    private PayrollCalculatorService payrollCalculatorService;

    private static final Long EMPLOYEE_ID = 1L;
    private static final int MES = 6;
    private static final int ANIO = 2026;

    @BeforeEach
    void setUp() {
        payrollCalculatorService = new PayrollCalculatorService(
                employeeRepository, attendanceService, commissionService, configurationService);
    }

    private void stubDiscountConfig() {
        given(configurationService.getDoubleConfig("descuento.jubilacion.porcentaje")).willReturn(11.0);
        given(configurationService.getDoubleConfig("descuento.obra_social.porcentaje")).willReturn(3.0);
        given(configurationService.getDoubleConfig("descuento.anses.porcentaje")).willReturn(3.0);
    }

    private void stubStandardScenario() {
        Employee employee = Employee.builder()
                .id(EMPLOYEE_ID)
                .salarioBase(new BigDecimal("500000"))
                .userId(1L)
                .build();

        AttendanceSummaryResponse attendance = AttendanceSummaryResponse.builder()
                .diasTrabajados(30)
                .horasTotalesMinutos(10800)
                .horasExtraMinutos(1200)
                .ausenciasInjustificadas(2)
                .build();

        CommissionResultResponse commission = CommissionResultResponse.builder()
                .comisionCalculada(new BigDecimal("25000"))
                .build();

        given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.of(employee));
        given(attendanceService.getSummary(EMPLOYEE_ID, MES, ANIO)).willReturn(attendance);
        given(commissionService.getSummary(EMPLOYEE_ID, MES, ANIO)).willReturn(commission);
        stubDiscountConfig();
    }

    @Test
    void calculate_WithAllData_ShouldReturnPayroll() {
        stubStandardScenario();

        Payroll result = payrollCalculatorService.calculate(EMPLOYEE_ID, MES, ANIO);

        assertThat(result.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
        assertThat(result.getMes()).isEqualTo(MES);
        assertThat(result.getAnio()).isEqualTo(ANIO);
        assertThat(result.getDiasTrabajados()).isEqualTo(30);
        assertThat(result.getHorasNormalesMinutos()).isEqualTo(9600);
        assertThat(result.getHorasExtraMinutos()).isEqualTo(1200);
        assertThat(result.getSueldoBasico()).isEqualByComparingTo(new BigDecimal("500000.00"));
        assertThat(result.getPlusHorasExtra()).isEqualByComparingTo(new BigDecimal("20833.30"));
        assertThat(result.getComisiones()).isEqualByComparingTo(new BigDecimal("25000.00"));
        assertThat(result.getBonoDesempeno()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getTotalHaberes()).isEqualByComparingTo(new BigDecimal("545833.30"));
        assertThat(result.getDescJubilacion()).isEqualByComparingTo(new BigDecimal("60041.66"));
        assertThat(result.getDescObraSocial()).isEqualByComparingTo(new BigDecimal("16375.00"));
        assertThat(result.getDescAnses()).isEqualByComparingTo(new BigDecimal("16375.00"));
        assertThat(result.getDescAusencias()).isEqualByComparingTo(new BigDecimal("33333.34"));
        assertThat(result.getDescEmbargos()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getTotalDescuentos()).isEqualByComparingTo(new BigDecimal("126125.00"));
        assertThat(result.getNetoApagar()).isEqualByComparingTo(new BigDecimal("419708.30"));
        assertThat(result.getEstado()).isEqualTo(Payroll.Estado.BORRADOR);
    }

    @Test
    void calculate_WhenEmployeeNotFound_ShouldThrow() {
        given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> payrollCalculatorService.calculate(EMPLOYEE_ID, MES, ANIO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("no encontrado");
    }

    @Test
    void calculate_WhenNoUserId_ShouldThrow() {
        Employee employee = Employee.builder()
                .id(EMPLOYEE_ID)
                .salarioBase(new BigDecimal("500000"))
                .userId(null)
                .build();

        given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.of(employee));

        assertThatThrownBy(() -> payrollCalculatorService.calculate(EMPLOYEE_ID, MES, ANIO))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no tiene un usuario vinculado");
    }

    @Test
    void calculate_WhenCommissionNull_ShouldUseZero() {
        Employee employee = Employee.builder()
                .id(EMPLOYEE_ID)
                .salarioBase(new BigDecimal("500000"))
                .userId(1L)
                .build();

        AttendanceSummaryResponse attendance = AttendanceSummaryResponse.builder()
                .diasTrabajados(30)
                .horasTotalesMinutos(14400)
                .horasExtraMinutos(0)
                .ausenciasInjustificadas(0)
                .build();

        given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.of(employee));
        given(attendanceService.getSummary(EMPLOYEE_ID, MES, ANIO)).willReturn(attendance);
        given(commissionService.getSummary(EMPLOYEE_ID, MES, ANIO)).willReturn(null);
        stubDiscountConfig();

        Payroll result = payrollCalculatorService.calculate(EMPLOYEE_ID, MES, ANIO);

        assertThat(result.getComisiones()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calculate_WhenNoAttendance_ShouldUseFullMonth() {
        Employee employee = Employee.builder()
                .id(EMPLOYEE_ID)
                .salarioBase(new BigDecimal("500000"))
                .userId(1L)
                .build();

        AttendanceSummaryResponse attendance = AttendanceSummaryResponse.builder()
                .diasTrabajados(0)
                .horasTotalesMinutos(0)
                .horasExtraMinutos(0)
                .ausenciasInjustificadas(0)
                .build();

        CommissionResultResponse commission = CommissionResultResponse.builder()
                .comisionCalculada(BigDecimal.ZERO)
                .build();

        given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.of(employee));
        given(attendanceService.getSummary(EMPLOYEE_ID, MES, ANIO)).willReturn(attendance);
        given(commissionService.getSummary(EMPLOYEE_ID, MES, ANIO)).willReturn(commission);
        stubDiscountConfig();

        Payroll result = payrollCalculatorService.calculate(EMPLOYEE_ID, MES, ANIO);

        assertThat(result.getDiasTrabajados()).isEqualTo(30);
        assertThat(result.getSueldoBasico()).isEqualByComparingTo(new BigDecimal("500000.00"));
    }

    @Test
    void calculate_WhenNetoNegative_ShouldFloorToZero() {
        Employee employee = Employee.builder()
                .id(EMPLOYEE_ID)
                .salarioBase(new BigDecimal("100000"))
                .userId(1L)
                .build();

        AttendanceSummaryResponse attendance = AttendanceSummaryResponse.builder()
                .diasTrabajados(30)
                .horasTotalesMinutos(14400)
                .horasExtraMinutos(0)
                .ausenciasInjustificadas(30)
                .build();

        CommissionResultResponse commission = CommissionResultResponse.builder()
                .comisionCalculada(BigDecimal.ZERO)
                .build();

        given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.of(employee));
        given(attendanceService.getSummary(EMPLOYEE_ID, MES, ANIO)).willReturn(attendance);
        given(commissionService.getSummary(EMPLOYEE_ID, MES, ANIO)).willReturn(commission);
        stubDiscountConfig();

        Payroll result = payrollCalculatorService.calculate(EMPLOYEE_ID, MES, ANIO);

        assertThat(result.getNetoApagar()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
