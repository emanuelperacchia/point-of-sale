package com.pos.system.controller;

import com.pos.system.dto.request.CreateReturnRequest;
import com.pos.system.dto.response.SaleReturnResponse;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.SaleReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
@Tag(name = "Devoluciones", description = "Devoluciones de ventas con reintegro de stock")
@SecurityRequirement(name = "bearerAuth")
public class SaleReturnController {

    private final SaleReturnService saleReturnService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO')")
    @Operation(summary = "Crear devolución", description = "Inicia una devolución parcial o total de una venta")
    public ResponseEntity<SaleReturnResponse> createReturn(
            @Valid @RequestBody CreateReturnRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        SaleReturnResponse response = saleReturnService.createReturn(request, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Aprobar devolución", description = "Aprueba una devolución pendiente y reintegra el stock")
    public ResponseEntity<SaleReturnResponse> approveReturn(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(saleReturnService.approveReturn(id, userDetails.getId()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Rechazar devolución", description = "Rechaza una devolución pendiente")
    public ResponseEntity<SaleReturnResponse> rejectReturn(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(saleReturnService.rejectReturn(id, userDetails.getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO', 'VENDEDOR')")
    @Operation(summary = "Obtener devolución", description = "Obtiene una devolución por su ID")
    public ResponseEntity<SaleReturnResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(saleReturnService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO', 'VENDEDOR')")
    @Operation(summary = "Listar devoluciones", description = "Lista devoluciones de una venta")
    public ResponseEntity<List<SaleReturnResponse>> findBySaleId(
            @RequestParam Long saleId) {
        return ResponseEntity.ok(saleReturnService.findBySaleId(saleId));
    }
}
