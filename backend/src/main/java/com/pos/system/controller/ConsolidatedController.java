package com.pos.system.controller;

import com.pos.system.dto.response.ConsolidatedReportResponse;
import com.pos.system.service.ConsolidatedReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports/consolidated")
@RequiredArgsConstructor
@Tag(name = "Reportes Consolidados", description = "Reportes multi-sucursal (solo ADMIN)")
public class ConsolidatedController {

    private final ConsolidatedReportService consolidatedReportService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reporte consolidado de todas las sucursales",
               description = "Solo accesible con rol ADMIN. Retorna métricas globales, por sucursal, productos críticos y transferencias.")
    public ResponseEntity<ConsolidatedReportResponse> getConsolidated(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(consolidatedReportService.getSummary(desde, hasta));
    }
}
