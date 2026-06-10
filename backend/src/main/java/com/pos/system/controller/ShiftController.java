package com.pos.system.controller;

import com.pos.system.dto.request.ShiftAssignmentRequest;
import com.pos.system.dto.request.ShiftChangeRequestDto;
import com.pos.system.dto.request.ShiftDefinitionRequest;
import com.pos.system.dto.response.ShiftAssignmentResponse;
import com.pos.system.dto.response.ShiftChangeRequestResponse;
import com.pos.system.dto.response.ShiftDefinitionResponse;
import com.pos.system.dto.response.ShiftScheduleResponse;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.ShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;

    // ── Definitions ──────────────────────────────────────────────────

    @GetMapping("/definitions")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<List<ShiftDefinitionResponse>> listDefinitions() {
        return ResponseEntity.ok(shiftService.listarDefiniciones());
    }

    @PostMapping("/definitions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShiftDefinitionResponse> createDefinition(
            @Valid @RequestBody ShiftDefinitionRequest request) {
        return ResponseEntity.ok(shiftService.crearDefinicion(request));
    }

    // ── Assignments ──────────────────────────────────────────────────

    @PostMapping("/assignments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShiftAssignmentResponse> assign(@Valid @RequestBody ShiftAssignmentRequest request) {
        return ResponseEntity.ok(shiftService.asignarTurno(request));
    }

    @GetMapping("/schedule")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<ShiftScheduleResponse> getSchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semana,
            @RequestParam(required = false) Long sucursalId) {
        return ResponseEntity.ok(shiftService.getSchedule(semana, sucursalId));
    }

    // ── Employee Shifts ─────────────────────────────────────────────

    @GetMapping("/employees/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<List<ShiftAssignmentResponse>> getEmployeeShifts(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semana) {
        return ResponseEntity.ok(shiftService.getAssignmentsByEmployee(employeeId, semana));
    }

    // ── Change Requests ─────────────────────────────────────────────

    @PostMapping("/change-requests")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','CAJERO','VENDEDOR')")
    public ResponseEntity<ShiftChangeRequestResponse> requestChange(
            @Valid @RequestBody ShiftChangeRequestDto request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(shiftService.solicitarCambio(userDetails.getUser().getId(), request));
    }

    @GetMapping("/change-requests/pending")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<List<ShiftChangeRequestResponse>> getPendingRequests() {
        return ResponseEntity.ok(shiftService.getSolicitudesPendientes());
    }

    @PutMapping("/change-requests/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShiftChangeRequestResponse> resolveRequest(
            @PathVariable Long id,
            @RequestParam boolean aprobado,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(shiftService.resolverSolicitud(id, aprobado, userDetails.getUser().getId()));
    }
}
