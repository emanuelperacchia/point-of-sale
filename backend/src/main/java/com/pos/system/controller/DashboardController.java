package com.pos.system.controller;

import com.pos.system.dto.response.ExecutiveDashboardResponse;
import com.pos.system.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard Ejecutivo (US-035)")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/executive")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Dashboard Ejecutivo con KPIs, gráficos y alertas")
    public ResponseEntity<ExecutiveDashboardResponse> getExecutiveDashboard(
            @RequestParam(defaultValue = "MONTH") String periodo,
            @RequestParam(required = false) Long sucursalId) {
        return ResponseEntity.ok(dashboardService.getExecutiveSummary(periodo, sucursalId));
    }
}
