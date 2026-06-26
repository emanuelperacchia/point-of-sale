package com.pos.system.controller;

import com.pos.system.dto.request.BranchPriceRequest;
import com.pos.system.dto.response.BranchPriceResponse;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.BranchPriceService;
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
@RequestMapping("/api/branches/{branchId}/prices")
@RequiredArgsConstructor
@Tag(name = "Precios por Sucursal", description = "Gestión de listas de precios diferenciadas por sucursal")
@SecurityRequirement(name = "bearerAuth")
public class BranchPriceController {

    private final BranchPriceService branchPriceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Crear precio local", description = "Define un precio local para un producto en una sucursal")
    public ResponseEntity<BranchPriceResponse> create(
            @PathVariable Long branchId,
            @Valid @RequestBody BranchPriceRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(branchPriceService.create(branchId, request, userDetails.getUser().getId()));
    }

    @GetMapping
    @Operation(summary = "Listar precios locales",
              description = "Retorna todos los precios locales activos de la sucursal con el precio global como referencia")
    public ResponseEntity<List<BranchPriceResponse>> getByBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(branchPriceService.getByBranch(branchId));
    }

    @GetMapping("/{priceId}")
    @Operation(summary = "Obtener precio local por ID")
    public ResponseEntity<BranchPriceResponse> getById(
            @PathVariable Long branchId, @PathVariable Long priceId) {
        return ResponseEntity.ok(branchPriceService.getById(branchId, priceId));
    }

    @PutMapping("/{priceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Actualizar precio local")
    public ResponseEntity<BranchPriceResponse> update(
            @PathVariable Long branchId,
            @PathVariable Long priceId,
            @Valid @RequestBody BranchPriceRequest request) {
        return ResponseEntity.ok(branchPriceService.update(branchId, priceId, request));
    }

    @DeleteMapping("/{priceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Eliminar precio local", description = "Desactiva lógicamente el precio local")
    public ResponseEntity<Void> delete(@PathVariable Long branchId, @PathVariable Long priceId) {
        branchPriceService.delete(branchId, priceId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sync-global")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Sincronizar con catálogo global",
              description = "Desactiva todos los precios locales de la sucursal, volviendo al precio global para todos los productos")
    public ResponseEntity<Void> syncGlobal(@PathVariable Long branchId) {
        branchPriceService.syncGlobal(branchId);
        return ResponseEntity.ok().build();
    }
}
