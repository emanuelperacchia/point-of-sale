package com.pos.system.service;

import com.pos.system.dto.request.ShiftAssignmentRequest;
import com.pos.system.dto.request.ShiftChangeRequestDto;
import com.pos.system.dto.request.ShiftDefinitionRequest;
import com.pos.system.dto.response.ShiftAssignmentResponse;
import com.pos.system.dto.response.ShiftChangeRequestResponse;
import com.pos.system.dto.response.ShiftDefinitionResponse;
import com.pos.system.entity.Employee;
import com.pos.system.entity.ShiftAssignment;
import com.pos.system.entity.ShiftChangeRequest;
import com.pos.system.entity.ShiftDefinition;
import com.pos.system.exception.BadRequestException;
import com.pos.system.repository.EmployeeRepository;
import com.pos.system.repository.ShiftAssignmentRepository;
import com.pos.system.repository.ShiftChangeRequestRepository;
import com.pos.system.repository.ShiftDefinitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShiftServiceTest {

    @Mock private ShiftDefinitionRepository shiftDefinitionRepository;
    @Mock private ShiftAssignmentRepository shiftAssignmentRepository;
    @Mock private ShiftChangeRequestRepository shiftChangeRequestRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private NotificationService notificationService;

    private ShiftService shiftService;

    private final Long employeeId = 100L;
    private final Long supervisorId = 200L;
    private final Long definitionId = 10L;
    private final Long assignmentId = 50L;
    private final Long requestId = 1L;
    private final LocalDate semana = LocalDate.of(2026, 6, 8);

    @BeforeEach
    void setUp() {
        shiftService = new ShiftService(
                shiftDefinitionRepository, shiftAssignmentRepository,
                shiftChangeRequestRepository, employeeRepository,
                notificationService);
    }

    @Test
    void crearDefinicion_ShouldCreateAndReturnResponse() {
        ShiftDefinitionRequest request = ShiftDefinitionRequest.builder()
                .nombre("Mañana")
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(12, 0))
                .diasSemana(31)
                .color("#FF0000")
                .build();

        ShiftDefinition savedDef = ShiftDefinition.builder()
                .id(definitionId)
                .nombre("Mañana")
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(12, 0))
                .diasSemana(31)
                .color("#FF0000")
                .activo(true)
                .build();

        when(shiftDefinitionRepository.save(any(ShiftDefinition.class))).thenReturn(savedDef);

        ShiftDefinitionResponse response = shiftService.crearDefinicion(request);

        assertNotNull(response);
        assertEquals(definitionId, response.getId());
        assertEquals("Mañana", response.getNombre());
        assertEquals(LocalTime.of(8, 0), response.getHoraInicio());
        assertEquals(LocalTime.of(12, 0), response.getHoraFin());
        assertEquals(31, response.getDiasSemana());
        assertEquals("#FF0000", response.getColor());
        assertTrue(response.getActivo());
    }

    @Test
    void asignarTurno_ShouldCreateAssignment() {
        Employee employee = Employee.builder()
                .id(employeeId)
                .nombre("Juan")
                .apellido("Pérez")
                .userId(10L)
                .build();

        ShiftDefinition def = ShiftDefinition.builder()
                .id(definitionId)
                .nombre("Mañana")
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(12, 0))
                .diasSemana(31)
                .color("#FF0000")
                .activo(true)
                .build();

        ShiftAssignmentRequest request = ShiftAssignmentRequest.builder()
                .employeeId(employeeId)
                .shiftDefinitionId(definitionId)
                .semana(semana)
                .diasActivos(List.of(1, 3, 5))
                .sucursalId(1L)
                .build();

        ShiftAssignment savedAssignment = ShiftAssignment.builder()
                .id(assignmentId)
                .employeeId(employeeId)
                .shiftDefinitionId(definitionId)
                .semana(semana)
                .diasActivos(21)
                .sucursalId(1L)
                .build();

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(shiftDefinitionRepository.findById(definitionId)).thenReturn(Optional.of(def));
        when(shiftAssignmentRepository.findByEmployeeIdAndSemana(employeeId, semana))
                .thenReturn(List.of());
        when(shiftAssignmentRepository.save(any(ShiftAssignment.class))).thenReturn(savedAssignment);

        ShiftAssignmentResponse response = shiftService.asignarTurno(request);

        assertNotNull(response);
        assertEquals(assignmentId, response.getId());
        assertEquals(employeeId, response.getEmployeeId());
        assertEquals("Juan Pérez", response.getEmployeeName());
        assertEquals(definitionId, response.getShiftDefinitionId());
        assertEquals("Mañana", response.getShiftName());
        assertEquals(LocalTime.of(8, 0), response.getHoraInicio());
        assertEquals(LocalTime.of(12, 0), response.getHoraFin());
        assertEquals(semana, response.getSemana());
        assertEquals(List.of(1, 3, 5), response.getDiasActivos());
        assertEquals(1L, response.getSucursalId());

        verify(notificationService).crear(eq(10L), anyString(), anyString());
    }

    @Test
    void asignarTurno_WhenOverlapDetected_ShouldThrowBadRequest() {
        Employee employee = Employee.builder()
                .id(employeeId)
                .nombre("Juan")
                .apellido("Pérez")
                .userId(10L)
                .build();

        ShiftDefinition newDef = ShiftDefinition.builder()
                .id(definitionId)
                .nombre("Mañana")
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(12, 0))
                .build();

        ShiftDefinition existingDef = ShiftDefinition.builder()
                .id(999L)
                .nombre("Tarde")
                .horaInicio(LocalTime.of(10, 0))
                .horaFin(LocalTime.of(14, 0))
                .build();

        ShiftAssignment existingAssignment = ShiftAssignment.builder()
                .id(99L)
                .employeeId(employeeId)
                .shiftDefinitionId(999L)
                .semana(semana)
                .diasActivos(7)
                .build();

        ShiftAssignmentRequest request = ShiftAssignmentRequest.builder()
                .employeeId(employeeId)
                .shiftDefinitionId(definitionId)
                .semana(semana)
                .diasActivos(List.of(1, 3, 5))
                .sucursalId(1L)
                .build();

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(shiftDefinitionRepository.findById(definitionId)).thenReturn(Optional.of(newDef));
        when(shiftAssignmentRepository.findByEmployeeIdAndSemana(employeeId, semana))
                .thenReturn(List.of(existingAssignment));
        when(shiftDefinitionRepository.findById(999L)).thenReturn(Optional.of(existingDef));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> shiftService.asignarTurno(request));
        assertTrue(ex.getMessage().contains("solapa"));

        verify(shiftAssignmentRepository, never()).save(any());
        verify(notificationService, never()).crear(any(), anyString(), anyString());
    }

    @Test
    void solicitarCambio_ShouldCreatePendingRequest() {
        ShiftAssignment assignment = ShiftAssignment.builder()
                .id(assignmentId)
                .employeeId(employeeId)
                .build();

        ShiftChangeRequestDto request = ShiftChangeRequestDto.builder()
                .assignmentId(assignmentId)
                .fechaOriginal(semana.plusDays(2))
                .motivo("Problema personal")
                .build();

        ShiftChangeRequest savedRequest = ShiftChangeRequest.builder()
                .id(requestId)
                .employeeId(employeeId)
                .assignmentId(assignmentId)
                .fechaOriginal(semana.plusDays(2))
                .motivo("Problema personal")
                .estado(ShiftChangeRequest.Estado.PENDIENTE)
                .createdAt(LocalDateTime.now())
                .build();

        when(shiftAssignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(shiftChangeRequestRepository.save(any(ShiftChangeRequest.class))).thenReturn(savedRequest);

        ShiftChangeRequestResponse response = shiftService.solicitarCambio(employeeId, request);

        assertNotNull(response);
        assertEquals(requestId, response.getId());
        assertEquals(employeeId, response.getEmployeeId());
        assertEquals(assignmentId, response.getAssignmentId());
        assertEquals(semana.plusDays(2), response.getFechaOriginal());
        assertEquals("Problema personal", response.getMotivo());
        assertEquals(ShiftChangeRequest.Estado.PENDIENTE, response.getEstado());
        assertNull(response.getRevisadoPor());
        assertNull(response.getFechaRevision());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    void resolverSolicitud_ShouldApproveAndNotify() {
        Employee employee = Employee.builder()
                .id(employeeId)
                .nombre("Juan")
                .apellido("Pérez")
                .userId(10L)
                .build();

        ShiftChangeRequest pendingRequest = ShiftChangeRequest.builder()
                .id(requestId)
                .employeeId(employeeId)
                .assignmentId(assignmentId)
                .fechaOriginal(semana.plusDays(2))
                .motivo("Problema personal")
                .estado(ShiftChangeRequest.Estado.PENDIENTE)
                .build();

        when(shiftChangeRequestRepository.findById(requestId)).thenReturn(Optional.of(pendingRequest));
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(shiftChangeRequestRepository.save(any(ShiftChangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShiftChangeRequestResponse response = shiftService.resolverSolicitud(requestId, true, supervisorId);

        assertEquals(ShiftChangeRequest.Estado.APROBADO, response.getEstado());
        assertEquals(supervisorId, response.getRevisadoPor());
        assertNotNull(response.getFechaRevision());

        verify(notificationService).crear(eq(10L), anyString(), anyString());
    }

    @Test
    void resolverSolicitud_ShouldApproveWithoutNotification_WhenEmployeeHasNoUserId() {
        Employee employee = Employee.builder()
                .id(employeeId)
                .nombre("Juan")
                .apellido("Pérez")
                .userId(null)
                .build();

        ShiftChangeRequest pendingRequest = ShiftChangeRequest.builder()
                .id(requestId)
                .employeeId(employeeId)
                .assignmentId(assignmentId)
                .fechaOriginal(semana.plusDays(2))
                .motivo("Problema personal")
                .estado(ShiftChangeRequest.Estado.PENDIENTE)
                .build();

        when(shiftChangeRequestRepository.findById(requestId)).thenReturn(Optional.of(pendingRequest));
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(shiftChangeRequestRepository.save(any(ShiftChangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShiftChangeRequestResponse response = shiftService.resolverSolicitud(requestId, true, supervisorId);

        assertEquals(ShiftChangeRequest.Estado.APROBADO, response.getEstado());

        verify(notificationService, never()).crear(any(), anyString(), anyString());
    }

    @Test
    void resolverSolicitud_ShouldReject() {
        Employee employee = Employee.builder()
                .id(employeeId)
                .nombre("Juan")
                .apellido("Pérez")
                .userId(10L)
                .build();

        ShiftChangeRequest pendingRequest = ShiftChangeRequest.builder()
                .id(requestId)
                .employeeId(employeeId)
                .assignmentId(assignmentId)
                .fechaOriginal(semana.plusDays(2))
                .motivo("Problema personal")
                .estado(ShiftChangeRequest.Estado.PENDIENTE)
                .build();

        when(shiftChangeRequestRepository.findById(requestId)).thenReturn(Optional.of(pendingRequest));
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(shiftChangeRequestRepository.save(any(ShiftChangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShiftChangeRequestResponse response = shiftService.resolverSolicitud(requestId, false, supervisorId);

        assertEquals(ShiftChangeRequest.Estado.RECHAZADO, response.getEstado());
        assertEquals(supervisorId, response.getRevisadoPor());
        assertNotNull(response.getFechaRevision());
    }
}
