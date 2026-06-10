package com.pos.system.service;

import com.pos.system.dto.request.EmployeeRequest;
import com.pos.system.dto.response.EmployeeHistoryResponse;
import com.pos.system.dto.response.EmployeeResponse;
import com.pos.system.entity.Employee;
import com.pos.system.entity.EmployeeHistory;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.EmployeeHistoryRepository;
import com.pos.system.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private EmployeeHistoryRepository historyRepository;

    private EmployeeService employeeService;

    private EmployeeRequest request;
    private Employee employee;
    private final Long modificadoPor = 1L;
    private final Long employeeId = 100L;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeService(employeeRepository, historyRepository);

        request = EmployeeRequest.builder()
                .nombre("Juan")
                .apellido("Pérez")
                .dni("12345678")
                .cuil("20-12345678-9")
                .fechaNacimiento(LocalDate.of(1990, 5, 15))
                .fechaIngreso(LocalDate.of(2024, 1, 1))
                .cargo("Vendedor")
                .departamento("Ventas")
                .sucursalId(1L)
                .salarioBase(BigDecimal.valueOf(500000))
                .modalidadContrato(Employee.ModalidadContrato.FULL_TIME)
                .userId(10L)
                .build();

        employee = Employee.builder()
                .id(employeeId)
                .nombre("Juan")
                .apellido("Pérez")
                .dni("12345678")
                .cuil("20-12345678-9")
                .fechaNacimiento(LocalDate.of(1990, 5, 15))
                .fechaIngreso(LocalDate.of(2024, 1, 1))
                .cargo("Vendedor")
                .departamento("Ventas")
                .sucursalId(1L)
                .salarioBase(BigDecimal.valueOf(500000))
                .modalidadContrato(Employee.ModalidadContrato.FULL_TIME)
                .userId(10L)
                .activo(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── crearEmpleado ────────────────────────────────────────────────────

    @Test
    void crearEmpleado_ShouldCreateAndReturnResponse() {
        when(employeeRepository.existsByDni(request.getDni())).thenReturn(false);
        when(employeeRepository.existsByUserId(request.getUserId())).thenReturn(false);
        when(employeeRepository.save(any())).thenReturn(employee);

        EmployeeResponse response = employeeService.crearEmpleado(request, modificadoPor);

        assertNotNull(response);
        assertEquals(employeeId, response.getId());
        assertEquals("Juan", response.getNombre());
        assertEquals("Vendedor", response.getCargo());
        assertTrue(response.getActivo());

        ArgumentCaptor<EmployeeHistory> captor = ArgumentCaptor.forClass(EmployeeHistory.class);
        verify(historyRepository, times(3)).save(captor.capture());

        List<EmployeeHistory> histories = captor.getAllValues();
        assertEquals(EmployeeHistory.Campo.CARGO, histories.get(0).getCampo());
        assertEquals("", histories.get(0).getValorAnterior());
        assertEquals("Vendedor", histories.get(0).getValorNuevo());

        assertEquals(EmployeeHistory.Campo.SALARIO, histories.get(1).getCampo());
        assertEquals("", histories.get(1).getValorAnterior());
        assertEquals("500000", histories.get(1).getValorNuevo());

        assertEquals(EmployeeHistory.Campo.DEPARTAMENTO, histories.get(2).getCampo());
        assertEquals("", histories.get(2).getValorAnterior());
        assertEquals("Ventas", histories.get(2).getValorNuevo());
    }

    @Test
    void crearEmpleado_WhenDniExists_ShouldThrowBadRequest() {
        when(employeeRepository.existsByDni(request.getDni())).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> employeeService.crearEmpleado(request, modificadoPor));
        assertTrue(ex.getMessage().contains("DNI"));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void crearEmpleado_WhenUserAlreadyLinked_ShouldThrowBadRequest() {
        when(employeeRepository.existsByDni(request.getDni())).thenReturn(false);
        when(employeeRepository.existsByUserId(request.getUserId())).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> employeeService.crearEmpleado(request, modificadoPor));
        assertTrue(ex.getMessage().contains("usuario"));
        verify(employeeRepository, never()).save(any());
    }

    // ── actualizarEmpleado ──────────────────────────────────────────────

    @Test
    void actualizarEmpleado_ShouldUpdateAndTrackHistory() {
        Employee existing = Employee.builder()
                .id(employeeId)
                .nombre("Juan")
                .apellido("Pérez")
                .dni("12345678")
                .cargo("Cajero")
                .departamento("Caja")
                .salarioBase(BigDecimal.valueOf(400000))
                .modalidadContrato(Employee.ModalidadContrato.FULL_TIME)
                .fechaNacimiento(LocalDate.of(1990, 5, 15))
                .fechaIngreso(LocalDate.of(2024, 1, 1))
                .activo(true)
                .build();

        EmployeeRequest updateReq = EmployeeRequest.builder()
                .nombre("Juan Carlos")
                .apellido("Pérez García")
                .dni("12345678")
                .fechaNacimiento(LocalDate.of(1990, 5, 15))
                .fechaIngreso(LocalDate.of(2024, 1, 1))
                .cargo("Vendedor")
                .departamento("Ventas")
                .sucursalId(1L)
                .salarioBase(BigDecimal.valueOf(500000))
                .modalidadContrato(Employee.ModalidadContrato.FULL_TIME)
                .build();

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(existing));
        when(employeeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeResponse response = employeeService.actualizarEmpleado(employeeId, updateReq, modificadoPor);

        assertNotNull(response);
        assertEquals("Juan Carlos", response.getNombre());
        assertEquals("Vendedor", response.getCargo());

        ArgumentCaptor<EmployeeHistory> captor = ArgumentCaptor.forClass(EmployeeHistory.class);
        verify(historyRepository, times(3)).save(captor.capture());

        List<EmployeeHistory> histories = captor.getAllValues();
        assertEquals(EmployeeHistory.Campo.CARGO, histories.get(0).getCampo());
        assertEquals("Cajero", histories.get(0).getValorAnterior());
        assertEquals("Vendedor", histories.get(0).getValorNuevo());

        assertEquals(EmployeeHistory.Campo.SALARIO, histories.get(1).getCampo());
        assertEquals("400000", histories.get(1).getValorAnterior());
        assertEquals("500000", histories.get(1).getValorNuevo());

        assertEquals(EmployeeHistory.Campo.DEPARTAMENTO, histories.get(2).getCampo());
        assertEquals("Caja", histories.get(2).getValorAnterior());
        assertEquals("Ventas", histories.get(2).getValorNuevo());
    }

    @Test
    void actualizarEmpleado_WhenNotFound_ShouldThrowResourceNotFound() {
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> employeeService.actualizarEmpleado(999L, request, modificadoPor));
    }

    // ── darDeBaja ────────────────────────────────────────────────────────

    @Test
    void darDeBaja_ShouldSetActivoFalseAndFechaBaja() {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        employeeService.darDeBaja(employeeId);

        assertFalse(employee.getActivo());
        assertNotNull(employee.getFechaBaja());
        assertEquals(LocalDate.now(), employee.getFechaBaja());
        verify(employeeRepository).save(employee);
    }

    // ── getById ──────────────────────────────────────────────────────────

    @Test
    void getById_ShouldReturnResponse() {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        EmployeeResponse response = employeeService.getById(employeeId);

        assertNotNull(response);
        assertEquals(employeeId, response.getId());
        assertEquals("Juan", response.getNombre());
        assertEquals("Vendedor", response.getCargo());
        assertEquals(BigDecimal.valueOf(500000), response.getSalarioBase());
    }

    @Test
    void getById_WhenNotFound_ShouldThrowResourceNotFound() {
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> employeeService.getById(999L));
    }

    // ── findByFilters ─────────────────────────────────────────────────────

    @Test
    void findByFilters_ShouldReturnFilteredList() {
        when(employeeRepository.findByFilters("Ventas", "Vendedor", 1L, true))
                .thenReturn(List.of(employee));

        List<EmployeeResponse> results = employeeService.findByFilters("Ventas", "Vendedor", 1L, true);

        assertEquals(1, results.size());
        assertEquals(employeeId, results.get(0).getId());
        assertEquals("Vendedor", results.get(0).getCargo());
        assertEquals("Ventas", results.get(0).getDepartamento());
    }

    // ── getHistory ───────────────────────────────────────────────────────

    @Test
    void getHistory_ShouldReturnHistoryList() {
        EmployeeHistory history = EmployeeHistory.builder()
                .id(1L)
                .employeeId(employeeId)
                .campo(EmployeeHistory.Campo.CARGO)
                .valorAnterior("Cajero")
                .valorNuevo("Vendedor")
                .fecha(LocalDateTime.now().minusDays(1))
                .modificadoPor(modificadoPor)
                .build();

        when(employeeRepository.existsById(employeeId)).thenReturn(true);
        when(historyRepository.findByEmployeeIdOrderByFechaDesc(employeeId))
                .thenReturn(List.of(history));

        List<EmployeeHistoryResponse> results = employeeService.getHistory(employeeId);

        assertEquals(1, results.size());
        assertEquals(EmployeeHistory.Campo.CARGO, results.get(0).getCampo());
        assertEquals("Cajero", results.get(0).getValorAnterior());
        assertEquals("Vendedor", results.get(0).getValorNuevo());
    }

    @Test
    void getHistory_WhenEmployeeNotFound_ShouldThrowResourceNotFound() {
        when(employeeRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> employeeService.getHistory(999L));
    }

    // ── actualizarDocumentoUrl ────────────────────────────────────────────

    @Test
    void actualizarDocumentoUrl_ShouldUpdateUrl() {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        employeeService.actualizarDocumentoUrl(employeeId, "https://docs.example.com/contrato.pdf");

        assertEquals("https://docs.example.com/contrato.pdf", employee.getDocumentoUrl());
        verify(employeeRepository).save(employee);
    }
}
