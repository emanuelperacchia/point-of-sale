package com.pos.system.controller;

import com.pos.system.dto.request.AbsenceRequest;
import com.pos.system.dto.request.CheckInRequest;
import com.pos.system.dto.request.CheckOutRequest;
import com.pos.system.dto.response.AbsenceResponse;
import com.pos.system.dto.response.AttendanceResponse;
import com.pos.system.dto.response.AttendanceSummaryResponse;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.AttendanceService;
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
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','CAJERO','VENDEDOR')")
    public ResponseEntity<AttendanceResponse> checkIn(@Valid @RequestBody CheckInRequest request) {
        return ResponseEntity.ok(attendanceService.checkIn(request));
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','CAJERO','VENDEDOR')")
    public ResponseEntity<AttendanceResponse> checkOut(@Valid @RequestBody CheckOutRequest request) {
        return ResponseEntity.ok(attendanceService.checkOut(request));
    }

    @PostMapping("/ausencias")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<AbsenceResponse> registrarAusencia(
            @Valid @RequestBody AbsenceRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(attendanceService.registrarAusencia(request, userDetails.getUser().getId()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<List<AttendanceResponse>> list(
            @RequestParam(required = false) Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(attendanceService.getAttendances(employeeId, desde, hasta));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<AttendanceSummaryResponse> summary(
            @RequestParam Long employeeId,
            @RequestParam int mes,
            @RequestParam int anio) {
        return ResponseEntity.ok(attendanceService.getSummary(employeeId, mes, anio));
    }
}
