package com.pos.system.controller;

import com.pos.system.dto.response.InventoryReportResponse;
import com.pos.system.dto.response.ProductAnalysisResponse;
import com.pos.system.dto.response.ProfitabilityResponse;
import com.pos.system.service.InventoryReportService;
import com.pos.system.service.ProductAnalysisService;
import com.pos.system.service.ProfitabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Tag(name = "Análisis", description = "Análisis de productos, rentabilidad y más")
@SecurityRequirement(name = "bearerAuth")
public class AnalysisController {

    private final ProductAnalysisService productAnalysisService;
    private final InventoryReportService inventoryReportService;
    private final ProfitabilityService profitabilityService;

    @GetMapping("/products-abc")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'INVENTARIO')")
    @Operation(summary = "Clasificación ABC de productos (Pareto)")
    public ResponseEntity<ProductAnalysisResponse> getProductABC(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "3") int meses,
            @RequestParam(defaultValue = "90") int diasSinVenta) {
        return ResponseEntity.ok(productAnalysisService.analyzeProducts(hasta, meses, diasSinVenta));
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'INVENTARIO')")
    @Operation(summary = "Reporte de inventario con valorización")
    public ResponseEntity<InventoryReportResponse> getInventoryReport() {
        return ResponseEntity.ok(inventoryReportService.getInventoryReport());
    }

    @GetMapping("/profitability")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CONTADOR')")
    @Operation(summary = "Análisis de rentabilidad con márgenes y punto de equilibrio")
    public ResponseEntity<ProfitabilityResponse> getProfitability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(profitabilityService.analyzeProfitability(desde, hasta));
    }
}
