package com.pos.system.service;

import com.pos.system.dto.request.ShiftAssignmentRequest;
import com.pos.system.dto.request.ShiftChangeRequestDto;
import com.pos.system.dto.request.ShiftDefinitionRequest;
import com.pos.system.dto.response.ShiftAssignmentResponse;
import com.pos.system.dto.response.ShiftChangeRequestResponse;
import com.pos.system.dto.response.ShiftDefinitionResponse;
import com.pos.system.dto.response.ShiftScheduleResponse;
import com.pos.system.entity.Employee;
import com.pos.system.entity.ShiftAssignment;
import com.pos.system.entity.ShiftChangeRequest;
import com.pos.system.entity.ShiftDefinition;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShiftService {

    private final ShiftDefinitionRepository shiftDefinitionRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftChangeRequestRepository shiftChangeRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;

    // ── Shift Definitions ────────────────────────────────────────────

    @Transactional
    public ShiftDefinitionResponse crearDefinicion(ShiftDefinitionRequest request) {
        ShiftDefinition def = ShiftDefinition.builder()
                .nombre(request.getNombre())
                .horaInicio(request.getHoraInicio())
                .horaFin(request.getHoraFin())
                .diasSemana(request.getDiasSemana())
                .color(request.getColor())
                .activo(true)
                .build();
        def = shiftDefinitionRepository.save(def);
        return mapToDefinitionResponse(def);
    }

    @Transactional(readOnly = true)
    public List<ShiftDefinitionResponse> listarDefiniciones() {
        return shiftDefinitionRepository.findByActivoTrue()
                .stream().map(this::mapToDefinitionResponse).toList();
    }

    // ── Shift Assignments ────────────────────────────────────────────

    @Transactional
    public ShiftAssignmentResponse asignarTurno(ShiftAssignmentRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado: " + request.getEmployeeId()));

        ShiftDefinition def = shiftDefinitionRepository.findById(request.getShiftDefinitionId())
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado: " + request.getShiftDefinitionId()));

        // Validate no overlap with existing assignments for same week
        List<ShiftAssignment> existing = shiftAssignmentRepository
                .findByEmployeeIdAndSemana(request.getEmployeeId(), request.getSemana());

        for (ShiftAssignment existingAss : existing) {
            ShiftDefinition existingDef = shiftDefinitionRepository.findById(existingAss.getShiftDefinitionId())
                    .orElse(null);
            if (existingDef != null && haySolapamiento(def.getHoraInicio(), def.getHoraFin(),
                    existingDef.getHoraInicio(), existingDef.getHoraFin())) {
                // Check if days overlap using bitmask
                int diasActivos = diasListToBitmask(request.getDiasActivos());
                if ((existingAss.getDiasActivos() & diasActivos) != 0) {
                    throw new BadRequestException("El empleado ya tiene un turno que se solapa en esta semana");
                }
            }
        }

        int diasActivosBitmask = diasListToBitmask(request.getDiasActivos());

        ShiftAssignment assignment = ShiftAssignment.builder()
                .employeeId(request.getEmployeeId())
                .shiftDefinitionId(request.getShiftDefinitionId())
                .semana(request.getSemana())
                .diasActivos(diasActivosBitmask)
                .sucursalId(request.getSucursalId())
                .build();
        assignment = shiftAssignmentRepository.save(assignment);

        // Notify employee
        notificationService.crear(employee.getUserId(),
                "Nuevo turno asignado",
                "Se te ha asignado el turno \"" + def.getNombre() + "\" para la semana del " + request.getSemana());

        return mapToAssignmentResponse(assignment, employee, def);
    }

    @Transactional(readOnly = true)
    public List<ShiftAssignmentResponse> getAssignmentsByEmployee(Long employeeId, LocalDate semana) {
        return shiftAssignmentRepository.findByEmployeeIdAndSemana(employeeId, semana)
                .stream().map(this::mapToAssignmentResponse).toList();
    }

    @Transactional(readOnly = true)
    public ShiftScheduleResponse getSchedule(LocalDate semana, Long sucursalId) {
        List<ShiftAssignment> assignments = shiftAssignmentRepository
                .findBySemanaAndSucursal(semana, sucursalId);

        // Group by employee
        Map<Long, List<ShiftAssignment>> byEmployee = assignments.stream()
                .collect(Collectors.groupingBy(ShiftAssignment::getEmployeeId));

        List<ShiftScheduleResponse.EmployeeSchedule> empleados = new ArrayList<>();

        for (Map.Entry<Long, List<ShiftAssignment>> entry : byEmployee.entrySet()) {
            Long empId = entry.getKey();
            Employee emp = employeeRepository.findById(empId).orElse(null);
            String empName = emp != null ? emp.getNombre() + " " + emp.getApellido() : "Desconocido";

            Map<Integer, List<ShiftScheduleResponse.ShiftInfo>> turnosPorDia = new HashMap<>();

            for (ShiftAssignment sa : entry.getValue()) {
                ShiftDefinition def = shiftDefinitionRepository.findById(sa.getShiftDefinitionId()).orElse(null);
                if (def == null) continue;

                // Extract active days from bitmask
                List<Integer> activeDays = bitmaskToDiasList(sa.getDiasActivos());
                for (int dia : activeDays) {
                    ShiftScheduleResponse.ShiftInfo info = ShiftScheduleResponse.ShiftInfo.builder()
                            .assignmentId(sa.getId())
                            .shiftDefinitionId(def.getId())
                            .shiftName(def.getNombre())
                            .horaInicio(def.getHoraInicio().toString())
                            .horaFin(def.getHoraFin().toString())
                            .color(def.getColor())
                            .build();
                    turnosPorDia.computeIfAbsent(dia, k -> new ArrayList<>()).add(info);
                }
            }

            empleados.add(ShiftScheduleResponse.EmployeeSchedule.builder()
                    .employeeId(empId)
                    .employeeName(empName)
                    .turnos(turnosPorDia)
                    .build());
        }

        return ShiftScheduleResponse.builder()
                .semana(semana)
                .sucursalId(sucursalId)
                .empleados(empleados)
                .build();
    }

    // ── Shift Change Requests ────────────────────────────────────────

    @Transactional
    public ShiftChangeRequestResponse solicitarCambio(Long employeeId, ShiftChangeRequestDto request) {
        ShiftAssignment assignment = shiftAssignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada"));

        ShiftChangeRequest changeRequest = ShiftChangeRequest.builder()
                .employeeId(employeeId)
                .assignmentId(request.getAssignmentId())
                .fechaOriginal(request.getFechaOriginal())
                .motivo(request.getMotivo())
                .estado(ShiftChangeRequest.Estado.PENDIENTE)
                .build();
        changeRequest = shiftChangeRequestRepository.save(changeRequest);
        return mapToChangeRequestResponse(changeRequest);
    }

    @Transactional
    public ShiftChangeRequestResponse resolverSolicitud(Long requestId, boolean aprobado, Long supervisorId) {
        ShiftChangeRequest changeRequest = shiftChangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de cambio no encontrada: " + requestId));

        if (changeRequest.getEstado() != ShiftChangeRequest.Estado.PENDIENTE) {
            throw new BadRequestException("La solicitud ya fue " + changeRequest.getEstado().name().toLowerCase());
        }

        changeRequest.setEstado(aprobado ? ShiftChangeRequest.Estado.APROBADO : ShiftChangeRequest.Estado.RECHAZADO);
        changeRequest.setRevisadoPor(supervisorId);
        changeRequest.setFechaRevision(LocalDateTime.now());
        changeRequest = shiftChangeRequestRepository.save(changeRequest);

        // Notify employee
        Employee employee = employeeRepository.findById(changeRequest.getEmployeeId()).orElse(null);
        if (employee != null && employee.getUserId() != null) {
            String msg = aprobado
                    ? "Tu solicitud de cambio de turno para el " + changeRequest.getFechaOriginal() + " fue APROBADA"
                    : "Tu solicitud de cambio de turno para el " + changeRequest.getFechaOriginal() + " fue RECHAZADA";
            notificationService.crear(employee.getUserId(), "Cambio de turno", msg);
        }

        return mapToChangeRequestResponse(changeRequest);
    }

    @Transactional(readOnly = true)
    public List<ShiftChangeRequestResponse> getSolicitudesPendientes() {
        return shiftChangeRequestRepository
                .findByEstadoOrderByCreatedAtAsc(ShiftChangeRequest.Estado.PENDIENTE)
                .stream().map(this::mapToChangeRequestResponse).toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private boolean haySolapamiento(java.time.LocalTime inicio1, java.time.LocalTime fin1,
                                     java.time.LocalTime inicio2, java.time.LocalTime fin2) {
        return inicio1.isBefore(fin2) && inicio2.isBefore(fin1);
    }

    private int diasListToBitmask(List<Integer> dias) {
        int mask = 0;
        for (int d : dias) {
            mask |= (1 << (d - 1)); // day 1 (Monday) → bit 0
        }
        return mask;
    }

    private List<Integer> bitmaskToDiasList(int bitmask) {
        List<Integer> dias = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            if ((bitmask & (1 << i)) != 0) {
                dias.add(i + 1);
            }
        }
        return dias;
    }

    private ShiftDefinitionResponse mapToDefinitionResponse(ShiftDefinition d) {
        return ShiftDefinitionResponse.builder()
                .id(d.getId()).nombre(d.getNombre())
                .horaInicio(d.getHoraInicio()).horaFin(d.getHoraFin())
                .diasSemana(d.getDiasSemana()).color(d.getColor())
                .activo(d.getActivo())
                .build();
    }

    private ShiftAssignmentResponse mapToAssignmentResponse(ShiftAssignment sa) {
        Employee emp = employeeRepository.findById(sa.getEmployeeId()).orElse(null);
        ShiftDefinition def = shiftDefinitionRepository.findById(sa.getShiftDefinitionId()).orElse(null);
        return mapToAssignmentResponse(sa, emp, def);
    }

    private ShiftAssignmentResponse mapToAssignmentResponse(ShiftAssignment sa, Employee emp, ShiftDefinition def) {
        return ShiftAssignmentResponse.builder()
                .id(sa.getId())
                .employeeId(sa.getEmployeeId())
                .employeeName(emp != null ? emp.getNombre() + " " + emp.getApellido() : "Desconocido")
                .shiftDefinitionId(sa.getShiftDefinitionId())
                .shiftName(def != null ? def.getNombre() : "Desconocido")
                .horaInicio(def != null ? def.getHoraInicio() : null)
                .horaFin(def != null ? def.getHoraFin() : null)
                .semana(sa.getSemana())
                .diasActivos(bitmaskToDiasList(sa.getDiasActivos()))
                .sucursalId(sa.getSucursalId())
                .build();
    }

    private ShiftChangeRequestResponse mapToChangeRequestResponse(ShiftChangeRequest r) {
        return ShiftChangeRequestResponse.builder()
                .id(r.getId()).employeeId(r.getEmployeeId())
                .assignmentId(r.getAssignmentId())
                .fechaOriginal(r.getFechaOriginal())
                .motivo(r.getMotivo())
                .estado(r.getEstado())
                .revisadoPor(r.getRevisadoPor())
                .fechaRevision(r.getFechaRevision())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
