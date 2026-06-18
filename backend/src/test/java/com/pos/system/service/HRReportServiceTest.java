package com.pos.system.service;

import com.pos.system.entity.Employee;
import com.pos.system.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HRReportServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private PayrollRepository payrollRepository;
    @Mock private AttendanceRecordRepository attendanceRecordRepository;
    @Mock private SaleRepository saleRepository;

    private HRReportService hrReportService;

    @BeforeEach
    void setUp() {
        hrReportService = new HRReportService(
                employeeRepository, payrollRepository, attendanceRecordRepository, saleRepository);
    }

    @Test
    void getHRReport_ShouldReturnReport() {
        var emp1 = Employee.builder().id(1L).nombre("Juan").apellido("Perez").activo(true).build();
        var emp2 = Employee.builder().id(2L).nombre("Maria").apellido("Lopez").activo(true).build();

        when(employeeRepository.findByActivoTrue()).thenReturn(List.of(emp1, emp2));
        when(payrollRepository.sumNetoByMesAndAnio(anyInt(), anyInt())).thenReturn(BigDecimal.valueOf(20000));
        when(attendanceRecordRepository.countAusenciasByPeriod(any(), any())).thenReturn(5L);
        when(saleRepository.findTopSellers(any(), any())).thenReturn(List.of());

        var res = hrReportService.getHRReport(6, 2026);

        assertNotNull(res);
        assertEquals("OK", res.getStatus());
        assertNotNull(res.getResumen());
        assertEquals(2, res.getResumen().getEmpleadosActivos());
        assertEquals(0, BigDecimal.valueOf(20000).compareTo(res.getResumen().getCostoLaboralMensual()));
        assertEquals(0, BigDecimal.valueOf(10000).compareTo(res.getResumen().getSalarioPromedio()));
        assertTrue(res.getResumen().getAusentismoPct() >= 0);
    }

    @Test
    void getHRReport_WhenRepoFails_ShouldReturnError() {
        when(employeeRepository.findByActivoTrue()).thenThrow(new RuntimeException("DB Error"));

        var res = hrReportService.getHRReport(6, 2026);
        assertEquals("ERROR", res.getStatus());
    }
}
