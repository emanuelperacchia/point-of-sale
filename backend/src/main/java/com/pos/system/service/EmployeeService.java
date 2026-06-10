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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeHistoryRepository historyRepository;

    @Transactional
    public EmployeeResponse crearEmpleado(EmployeeRequest request, Long modificadoPor) {
        if (employeeRepository.existsByDni(request.getDni())) {
            throw new BadRequestException("Ya existe un empleado con el DNI: " + request.getDni());
        }
        if (request.getUserId() != null && employeeRepository.existsByUserId(request.getUserId())) {
            throw new BadRequestException("El usuario ya está vinculado a otro empleado");
        }

        Employee employee = Employee.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .dni(request.getDni())
                .cuil(request.getCuil())
                .fechaNacimiento(request.getFechaNacimiento())
                .fechaIngreso(request.getFechaIngreso())
                .cargo(request.getCargo())
                .departamento(request.getDepartamento())
                .sucursalId(request.getSucursalId())
                .salarioBase(request.getSalarioBase())
                .modalidadContrato(request.getModalidadContrato())
                .userId(request.getUserId())
                .activo(true)
                .build();

        employee = employeeRepository.save(employee);
        registrarHistorial(employee.getId(), EmployeeHistory.Campo.CARGO, null, request.getCargo(), modificadoPor);
        registrarHistorial(employee.getId(), EmployeeHistory.Campo.SALARIO, null, request.getSalarioBase().toString(), modificadoPor);
        if (request.getDepartamento() != null) {
            registrarHistorial(employee.getId(), EmployeeHistory.Campo.DEPARTAMENTO, null, request.getDepartamento(), modificadoPor);
        }

        return mapToResponse(employee);
    }

    @Transactional
    public EmployeeResponse actualizarEmpleado(Long id, EmployeeRequest request, Long modificadoPor) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado: " + id));

        // Track history for audited fields
        if (!employee.getCargo().equals(request.getCargo())) {
            registrarHistorial(id, EmployeeHistory.Campo.CARGO, employee.getCargo(), request.getCargo(), modificadoPor);
        }
        if (employee.getSalarioBase().compareTo(request.getSalarioBase()) != 0) {
            registrarHistorial(id, EmployeeHistory.Campo.SALARIO, employee.getSalarioBase().toString(), request.getSalarioBase().toString(), modificadoPor);
        }
        if (!employee.getDepartamento().equals(request.getDepartamento())) {
            registrarHistorial(id, EmployeeHistory.Campo.DEPARTAMENTO, employee.getDepartamento(), request.getDepartamento(), modificadoPor);
        }

        employee.setNombre(request.getNombre());
        employee.setApellido(request.getApellido());
        if (request.getCuil() != null) employee.setCuil(request.getCuil());
        employee.setFechaNacimiento(request.getFechaNacimiento());
        employee.setFechaIngreso(request.getFechaIngreso());
        employee.setCargo(request.getCargo());
        employee.setDepartamento(request.getDepartamento());
        employee.setSucursalId(request.getSucursalId());
        employee.setSalarioBase(request.getSalarioBase());
        employee.setModalidadContrato(request.getModalidadContrato());
        if (request.getUserId() != null) {
            employee.setUserId(request.getUserId());
        }

        employee = employeeRepository.save(employee);
        return mapToResponse(employee);
    }

    @Transactional
    public void darDeBaja(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado: " + id));
        employee.setActivo(false);
        employee.setFechaBaja(LocalDate.now());
        employeeRepository.save(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado: " + id));
        return mapToResponse(employee);
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> findByFilters(String departamento, String cargo, Long sucursalId, Boolean activo) {
        return employeeRepository.findByFilters(departamento, cargo, sucursalId, activo)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public void actualizarDocumentoUrl(Long id, String documentoUrl) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado: " + id));
        employee.setDocumentoUrl(documentoUrl);
        employeeRepository.save(employee);
    }

    @Transactional(readOnly = true)
    public List<EmployeeHistoryResponse> getHistory(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Empleado no encontrado: " + employeeId);
        }
        return historyRepository.findByEmployeeIdOrderByFechaDesc(employeeId)
                .stream().map(this::mapToHistoryResponse).toList();
    }

    private void registrarHistorial(Long employeeId, EmployeeHistory.Campo campo, String valorAnterior, String valorNuevo, Long modificadoPor) {
        EmployeeHistory history = EmployeeHistory.builder()
                .employeeId(employeeId)
                .campo(campo)
                .valorAnterior(valorAnterior != null ? valorAnterior : "")
                .valorNuevo(valorNuevo)
                .fecha(LocalDateTime.now())
                .modificadoPor(modificadoPor)
                .build();
        historyRepository.save(history);
    }

    private EmployeeResponse mapToResponse(Employee e) {
        return EmployeeResponse.builder()
                .id(e.getId())
                .nombre(e.getNombre())
                .apellido(e.getApellido())
                .dni(e.getDni())
                .cuil(e.getCuil())
                .fechaNacimiento(e.getFechaNacimiento())
                .fechaIngreso(e.getFechaIngreso())
                .fechaBaja(e.getFechaBaja())
                .cargo(e.getCargo())
                .departamento(e.getDepartamento())
                .sucursalId(e.getSucursalId())
                .salarioBase(e.getSalarioBase())
                .modalidadContrato(e.getModalidadContrato())
                .userId(e.getUserId())
                .activo(e.getActivo())
                .documentoUrl(e.getDocumentoUrl())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private EmployeeHistoryResponse mapToHistoryResponse(EmployeeHistory h) {
        return EmployeeHistoryResponse.builder()
                .id(h.getId())
                .employeeId(h.getEmployeeId())
                .campo(h.getCampo())
                .valorAnterior(h.getValorAnterior())
                .valorNuevo(h.getValorNuevo())
                .fecha(h.getFecha())
                .modificadoPor(h.getModificadoPor())
                .build();
    }
}
