package com.pos.system.controller;

import com.pos.system.dto.response.PriceDifferenceReportResponse;
import com.pos.system.service.PriceDifferenceReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Reportes del sistema")
@SecurityRequirement(name = "bearerAuth")
public class PriceDifferenceController {

    private final PriceDifferenceReportService priceDifferenceReportService;

    @GetMapping("/price-differences")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Reporte de diferencias de precios",
              description = "Muestra productos con precio local diferente al global, con la diferencia en % y monto")
    public ResponseEntity<List<PriceDifferenceReportResponse>> getPriceDifferences(
            @RequestParam Long branchId) {
        return ResponseEntity.ok(priceDifferenceReportService.getDifferences(branchId));
    }
}
