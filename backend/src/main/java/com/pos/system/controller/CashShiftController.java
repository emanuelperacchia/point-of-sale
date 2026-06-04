package com.pos.system.controller;

import com.pos.system.dto.response.ShiftMovementResponse;
import com.pos.system.dto.response.ShiftReportResponse;
import com.pos.system.dto.response.ShiftResponse;
import com.pos.system.entity.ShiftStatus;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.CashShiftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
@Tag(name = "Turnos de Caja", description = "Apertura, cierre y control de turnos")
@SecurityRequirement(name = "bearerAuth")
public class CashShiftController {

    private final CashShiftService cashShiftService;

    @PostMapping("/open")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO')")
    @Operation(summary = "Abrir turno", description = "Abre un turno de caja con el monto inicial declarado")
    public ResponseEntity<ShiftResponse> openShift(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long sucursalId = request.get("sucursalId") != null
                ? ((Number) request.get("sucursalId")).longValue()
                : 1L;
        BigDecimal montoApertura = new BigDecimal(request.get("montoApertura").toString());
        ShiftResponse response = cashShiftService.openShift(
                userDetails.getId(), sucursalId, montoApertura);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO')")
    @Operation(summary = "Cerrar turno", description = "Cierra el turno con el monto final declarado y calcula la diferencia")
    public ResponseEntity<ShiftResponse> closeShift(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        BigDecimal montoCierre = new BigDecimal(request.get("montoCierre").toString());
        return ResponseEntity.ok(cashShiftService.closeShift(id, montoCierre));
    }

    @PostMapping("/{id}/movements")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO')")
    @Operation(summary = "Registrar movimiento", description = "Registra un retiro o ingreso manual de caja")
    public ResponseEntity<ShiftMovementResponse> addMovement(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        String tipo = (String) request.get("tipo");
        BigDecimal monto = new BigDecimal(request.get("monto").toString());
        String motivo = (String) request.get("motivo");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cashShiftService.addMovement(id, tipo, monto, motivo, userDetails.getId()));
    }

    @GetMapping("/{id}/report")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO')")
    @Operation(summary = "Reporte de turno", description = "Obtiene el reporte detallado con ventas, movimientos y diferencia")
    public ResponseEntity<ShiftReportResponse> getReport(@PathVariable Long id) {
        return ResponseEntity.ok(cashShiftService.getReport(id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO')")
    @Operation(summary = "Obtener turno", description = "Obtiene un turno por su ID")
    public ResponseEntity<ShiftResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(cashShiftService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Listar turnos", description = "Lista el historial de turnos con filtros opcionales")
    public ResponseEntity<List<ShiftResponse>> findByFilters(
            @RequestParam(required = false) Long cajeroId,
            @RequestParam(required = false) ShiftStatus estado) {
        return ResponseEntity.ok(cashShiftService.findByFilters(cajeroId, estado));
    }
}
