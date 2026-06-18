package com.pos.system.controller;

import com.pos.system.dto.response.HRReportResponse;
import com.pos.system.service.HRReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
@Tag(name = "RRHH", description = "Reportes de Recursos Humanos (US-040)")
@SecurityRequirement(name = "bearerAuth")
public class HRReportController {

    private final HRReportService hrReportService;

    @GetMapping("/report")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Reporte de RRHH con ausentismo y productividad")
    public ResponseEntity<HRReportResponse> getHRReport(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio) {
        return ResponseEntity.ok(hrReportService.getHRReport(mes, anio));
    }
}
