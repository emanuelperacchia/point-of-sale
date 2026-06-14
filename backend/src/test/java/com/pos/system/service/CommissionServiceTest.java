package com.pos.system.service;

import com.pos.system.dto.request.CommissionSchemeRequest;
import com.pos.system.dto.response.CommissionResultResponse;
import com.pos.system.dto.response.CommissionSchemeResponse;
import com.pos.system.entity.*;
import com.pos.system.exception.BadRequestException;
import com.pos.system.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommissionServiceTest {

    @Mock private CommissionSchemeRepository schemeRepository;
    @Mock private CommissionTierRepository tierRepository;
    @Mock private EmployeeCommissionAssignmentRepository assignmentRepository;
    @Mock private SalesTargetRepository targetRepository;
    @Mock private CommissionResultRepository resultRepository;
    @Mock private SaleRepository saleRepository;
    @Mock private EmployeeRepository employeeRepository;

    private CommissionService commissionService;

    private final Long employeeId = 1L;
    private final int mes = 6;
    private final int anio = 2026;
    private final LocalDate periodStart = LocalDate.of(anio, mes, 1);
    private final LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
    private final LocalDateTime desde = periodStart.atStartOfDay();
    private final LocalDateTime hasta = periodEnd.atTime(LocalTime.MAX);

    @BeforeEach
    void setUp() {
        commissionService = new CommissionService(
                schemeRepository, tierRepository, assignmentRepository,
                targetRepository, resultRepository, saleRepository, employeeRepository);
    }

    @Test
    void crearEsquema_WithTiers_ShouldCreateEscalonado() {
        List<CommissionSchemeRequest.TierRequest> tierRequests = List.of(
                CommissionSchemeRequest.TierRequest.builder()
                        .montoDesde(BigDecimal.ZERO)
                        .montoHasta(BigDecimal.valueOf(50000))
                        .porcentaje(BigDecimal.valueOf(2))
                        .build(),
                CommissionSchemeRequest.TierRequest.builder()
                        .montoDesde(BigDecimal.valueOf(50000))
                        .montoHasta(BigDecimal.valueOf(100000))
                        .porcentaje(BigDecimal.valueOf(3))
                        .build(),
                CommissionSchemeRequest.TierRequest.builder()
                        .montoDesde(BigDecimal.valueOf(100000))
                        .montoHasta(null)
                        .porcentaje(BigDecimal.valueOf(5))
                        .build()
        );

        CommissionSchemeRequest request = CommissionSchemeRequest.builder()
                .nombre("Escalonado Test")
                .tipo(CommissionScheme.Tipo.ESCALONADO)
                .vigenciaDesde(LocalDate.of(2026, 1, 1))
                .tiers(tierRequests)
                .build();

        CommissionScheme savedScheme = CommissionScheme.builder()
                .id(1L)
                .nombre("Escalonado Test")
                .tipo(CommissionScheme.Tipo.ESCALONADO)
                .activo(true)
                .vigenciaDesde(LocalDate.of(2026, 1, 1))
                .build();

        List<CommissionTier> savedTiers = List.of(
                CommissionTier.builder().id(1L).schemeId(1L)
                        .montoDesde(BigDecimal.ZERO).montoHasta(BigDecimal.valueOf(50000))
                        .porcentaje(BigDecimal.valueOf(2)).build(),
                CommissionTier.builder().id(2L).schemeId(1L)
                        .montoDesde(BigDecimal.valueOf(50000)).montoHasta(BigDecimal.valueOf(100000))
                        .porcentaje(BigDecimal.valueOf(3)).build(),
                CommissionTier.builder().id(3L).schemeId(1L)
                        .montoDesde(BigDecimal.valueOf(100000)).montoHasta(null)
                        .porcentaje(BigDecimal.valueOf(5)).build()
        );

        when(schemeRepository.save(any(CommissionScheme.class))).thenReturn(savedScheme);
        when(tierRepository.findBySchemeIdOrderByMontoDesdeAsc(1L)).thenReturn(savedTiers);

        CommissionSchemeResponse response = commissionService.crearEsquema(request);

        assertNotNull(response);
        assertEquals("Escalonado Test", response.getNombre());
        assertEquals(CommissionScheme.Tipo.ESCALONADO, response.getTipo());
        assertTrue(response.getActivo());
        assertEquals(3, response.getTiers().size());
        assertEquals(BigDecimal.valueOf(2), response.getTiers().get(0).getPorcentaje());
        assertEquals(BigDecimal.valueOf(5), response.getTiers().get(2).getPorcentaje());

        verify(schemeRepository).save(any(CommissionScheme.class));
        verify(tierRepository).saveAll(anyList());
        verify(tierRepository).findBySchemeIdOrderByMontoDesdeAsc(1L);
    }

    @Test
    void calculate_WithPorcentajeVenta_ShouldCalculate() {
        Employee employee = Employee.builder().id(employeeId).userId(1L).build();
        EmployeeCommissionAssignment assignment = EmployeeCommissionAssignment.builder()
                .id(1L).employeeId(employeeId).schemeId(10L).build();
        CommissionScheme scheme = CommissionScheme.builder()
                .id(10L).nombre("5% Ventas")
                .tipo(CommissionScheme.Tipo.PORCENTAJE_VENTA)
                .valor(BigDecimal.valueOf(5))
                .build();

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(assignmentRepository.findVigente(employeeId, periodStart)).thenReturn(Optional.of(assignment));
        when(schemeRepository.findById(10L)).thenReturn(Optional.of(scheme));
        when(saleRepository.sumTotalByUserAndDateRange(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.valueOf(100000));
        when(targetRepository.findByEmployeeIdAndMesAndAnio(employeeId, mes, anio)).thenReturn(Optional.empty());
        when(resultRepository.save(any(CommissionResult.class))).thenAnswer(inv -> inv.getArgument(0));

        CommissionResultResponse response = commissionService.calculate(employeeId, mes, anio);

        assertNotNull(response);
        assertEquals(employeeId, response.getEmployeeId());
        assertEquals(BigDecimal.valueOf(100000), response.getTotalVentas());
        assertEquals(new BigDecimal("5000.00"), response.getComisionCalculada());
        assertFalse(response.getMetaAlcanzada());
        assertEquals(BigDecimal.ZERO, response.getBonoAplicado());
        assertEquals("5% Ventas", response.getEsquemaUsado());
    }

    @Test
    void calculate_WithEscalonado_ShouldApplyTiers() {
        Employee employee = Employee.builder().id(employeeId).userId(1L).build();
        EmployeeCommissionAssignment assignment = EmployeeCommissionAssignment.builder()
                .id(1L).employeeId(employeeId).schemeId(10L).build();
        CommissionScheme scheme = CommissionScheme.builder()
                .id(10L).nombre("Escalonado")
                .tipo(CommissionScheme.Tipo.ESCALONADO)
                .build();

        List<CommissionTier> tiers = List.of(
                CommissionTier.builder().schemeId(10L)
                        .montoDesde(BigDecimal.ZERO).montoHasta(BigDecimal.valueOf(50000))
                        .porcentaje(BigDecimal.valueOf(2)).build(),
                CommissionTier.builder().schemeId(10L)
                        .montoDesde(BigDecimal.valueOf(50000)).montoHasta(BigDecimal.valueOf(100000))
                        .porcentaje(BigDecimal.valueOf(3)).build(),
                CommissionTier.builder().schemeId(10L)
                        .montoDesde(BigDecimal.valueOf(100000)).montoHasta(null)
                        .porcentaje(BigDecimal.valueOf(5)).build()
        );

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(assignmentRepository.findVigente(employeeId, periodStart)).thenReturn(Optional.of(assignment));
        when(schemeRepository.findById(10L)).thenReturn(Optional.of(scheme));
        when(saleRepository.sumTotalByUserAndDateRange(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.valueOf(120000));
        when(tierRepository.findBySchemeIdOrderByMontoDesdeAsc(10L)).thenReturn(tiers);
        when(targetRepository.findByEmployeeIdAndMesAndAnio(employeeId, mes, anio)).thenReturn(Optional.empty());
        when(resultRepository.save(any(CommissionResult.class))).thenAnswer(inv -> inv.getArgument(0));

        CommissionResultResponse response = commissionService.calculate(employeeId, mes, anio);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(120000), response.getTotalVentas());
        assertEquals(new BigDecimal("3500.00"), response.getComisionCalculada());
        assertFalse(response.getMetaAlcanzada());
        assertEquals("Escalonado", response.getEsquemaUsado());
    }

    @Test
    void calculate_WithTargetBonus_ShouldAddBono() {
        Employee employee = Employee.builder().id(employeeId).userId(1L).build();
        EmployeeCommissionAssignment assignment = EmployeeCommissionAssignment.builder()
                .id(1L).employeeId(employeeId).schemeId(10L).build();
        CommissionScheme scheme = CommissionScheme.builder()
                .id(10L).nombre("5% Ventas")
                .tipo(CommissionScheme.Tipo.PORCENTAJE_VENTA)
                .valor(BigDecimal.valueOf(5))
                .build();

        SalesTarget target = SalesTarget.builder()
                .id(1L).employeeId(employeeId).mes(mes).anio(anio)
                .metaMonto(BigDecimal.valueOf(100000))
                .bonoPorSuperacion(SalesTarget.TipoBono.FIJO)
                .valorBono(BigDecimal.valueOf(2000))
                .build();

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(assignmentRepository.findVigente(employeeId, periodStart)).thenReturn(Optional.of(assignment));
        when(schemeRepository.findById(10L)).thenReturn(Optional.of(scheme));
        when(saleRepository.sumTotalByUserAndDateRange(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.valueOf(150000));
        when(targetRepository.findByEmployeeIdAndMesAndAnio(employeeId, mes, anio)).thenReturn(Optional.of(target));
        when(resultRepository.save(any(CommissionResult.class))).thenAnswer(inv -> inv.getArgument(0));

        CommissionResultResponse response = commissionService.calculate(employeeId, mes, anio);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(150000), response.getTotalVentas());
        assertEquals(new BigDecimal("9500.00"), response.getComisionCalculada());
        assertTrue(response.getMetaAlcanzada());
        assertEquals(BigDecimal.valueOf(2000), response.getBonoAplicado());
    }

    @Test
    void calculate_WhenNoUserId_ShouldThrowBadRequest() {
        Employee employee = Employee.builder().id(employeeId).userId(null).build();

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> commissionService.calculate(employeeId, mes, anio));
        assertTrue(ex.getMessage().contains("empleado no está vinculado"));
        verify(assignmentRepository, never()).findVigente(any(), any());
        verify(resultRepository, never()).save(any());
    }

    @Test
    void calculate_WhenNoSchemeAssigned_ShouldThrowBadRequest() {
        Employee employee = Employee.builder().id(employeeId).userId(1L).build();

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(assignmentRepository.findVigente(employeeId, periodStart)).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> commissionService.calculate(employeeId, mes, anio));
        assertTrue(ex.getMessage().contains("esquema de comisión vigente"));
        verify(resultRepository, never()).save(any());
    }

    @Test
    void getSummary_ShouldReturnSavedResult() {
        CommissionResult result = CommissionResult.builder()
                .id(99L).employeeId(employeeId).mes(mes).anio(anio)
                .totalVentas(BigDecimal.valueOf(100000))
                .comisionCalculada(BigDecimal.valueOf(5000))
                .metaAlcanzada(true)
                .bonoAplicado(BigDecimal.valueOf(2000))
                .esquemaUsado("5% Ventas")
                .build();

        when(resultRepository.findByEmployeeIdAndMesAndAnio(employeeId, mes, anio))
                .thenReturn(Optional.of(result));

        CommissionResultResponse response = commissionService.getSummary(employeeId, mes, anio);

        assertNotNull(response);
        assertEquals(99L, response.getId());
        assertEquals(employeeId, response.getEmployeeId());
        assertEquals(mes, response.getMes());
        assertEquals(anio, response.getAnio());
        assertEquals(BigDecimal.valueOf(100000), response.getTotalVentas());
        assertEquals(BigDecimal.valueOf(5000), response.getComisionCalculada());
        assertTrue(response.getMetaAlcanzada());
        assertEquals(BigDecimal.valueOf(2000), response.getBonoAplicado());
        assertEquals("5% Ventas", response.getEsquemaUsado());
    }

    @Test
    void getRanking_ShouldReturnSortedResults() {
        CommissionResult first = CommissionResult.builder()
                .id(1L).employeeId(2L).mes(mes).anio(anio)
                .totalVentas(BigDecimal.valueOf(200000))
                .comisionCalculada(BigDecimal.valueOf(10000))
                .build();
        CommissionResult second = CommissionResult.builder()
                .id(2L).employeeId(1L).mes(mes).anio(anio)
                .totalVentas(BigDecimal.valueOf(100000))
                .comisionCalculada(BigDecimal.valueOf(5000))
                .build();

        when(resultRepository.findByMesAndAnioOrderByTotalVentasDesc(mes, anio))
                .thenReturn(List.of(first, second));

        List<CommissionResultResponse> ranking = commissionService.getRanking(mes, anio);

        assertEquals(2, ranking.size());
        assertEquals(2L, ranking.get(0).getEmployeeId());
        assertEquals(BigDecimal.valueOf(200000), ranking.get(0).getTotalVentas());
        assertEquals(1L, ranking.get(1).getEmployeeId());
        assertEquals(BigDecimal.valueOf(100000), ranking.get(1).getTotalVentas());
    }
}
