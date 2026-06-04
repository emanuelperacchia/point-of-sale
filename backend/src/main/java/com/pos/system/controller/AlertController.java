package com.pos.system.controller;

import com.pos.system.dto.response.AlertResponse;
import com.pos.system.dto.response.LowStockReportResponse;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/alerts")
@RequiredArgsConstructor
@Tag(name = "Alertas", description = "Gestión de alertas de inventario")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @Operation(summary = "Alertas activas", description = "Obtiene todas las alertas activas (no resueltas)")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Page<AlertResponse>> getActiveAlerts(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(alertService.getActiveAlerts(pageable));
    }

    @GetMapping("/unread")
    @Operation(summary = "Alertas no leídas", description = "Obtiene alertas no leídas")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<AlertResponse>> getUnreadAlerts() {
        return ResponseEntity.ok(alertService.getUnreadAlerts());
    }

    @GetMapping("/count")
    @Operation(summary = "Contador de alertas", description = "Cantidad de alertas activas y no leídas")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<java.util.Map<String, Long>> getAlertCounts() {
        return ResponseEntity.ok(java.util.Map.of(
                "active", alertService.countActiveAlerts(),
                "unread", alertService.countUnreadAlerts()
        ));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Marcar como leída", description = "Marca una alerta como leída")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<AlertResponse> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.markAsRead(id));
    }

    @PutMapping("/{id}/resolve")
    @Operation(summary = "Resolver alerta", description = "Resuelve una alerta")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<AlertResponse> resolveAlert(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(alertService.resolveAlert(id, userDetails.getId()));
    }

    @GetMapping("/report")
    @Operation(summary = "Reporte de stock bajo", description = "Reporte de productos bajo stock mínimo")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<LowStockReportResponse> getLowStockReport() {
        return ResponseEntity.ok(alertService.getLowStockReport());
    }
}