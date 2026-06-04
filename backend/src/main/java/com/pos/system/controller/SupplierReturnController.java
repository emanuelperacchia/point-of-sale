package com.pos.system.controller;

import com.pos.system.dto.request.SupplierReturnRequest;
import com.pos.system.dto.response.SupplierReturnResponse;
import com.pos.system.entity.SupplierReturn;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.SupplierReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para devoluciones a proveedores.
 */
@RestController
@RequestMapping("/api/supplier-returns")
@RequiredArgsConstructor
@Tag(name = "Supplier Returns", description = "API de devoluciones a proveedores")
public class SupplierReturnController {

    private final SupplierReturnService supplierReturnService;

    @GetMapping
    @PreAuthorize("hasAuthority('COMPRAS_READ')")
    @Operation(summary = "Listar todas las devoluciones")
    public ResponseEntity<List<SupplierReturnResponse>> findAll() {
        return ResponseEntity.ok(supplierReturnService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COMPRAS_READ')")
    @Operation(summary = "Obtener devolución por ID")
    public ResponseEntity<SupplierReturnResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(supplierReturnService.findById(id));
    }

    @GetMapping("/by-supplier/{supplierId}")
    @PreAuthorize("hasAuthority('COMPRAS_READ')")
    @Operation(summary = "Obtener devoluciones por proveedor")
    public ResponseEntity<List<SupplierReturnResponse>> findBySupplier(
            @PathVariable Long supplierId) {
        return ResponseEntity.ok(supplierReturnService.findBySupplier(supplierId));
    }

    @GetMapping("/by-status/{status}")
    @PreAuthorize("hasAuthority('COMPRAS_READ')")
    @Operation(summary = "Obtener devoluciones por estado")
    public ResponseEntity<List<SupplierReturnResponse>> findByStatus(
            @PathVariable SupplierReturn.ReturnStatus status) {
        return ResponseEntity.ok(supplierReturnService.findByStatus(status));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('COMPRAS_CREATE')")
    @Operation(summary = "Registrar devolución a proveedor",
            description = "Registra la devolución, descuenta stock y genera nota de crédito automática")
    public ResponseEntity<SupplierReturnResponse> create(
            @Valid @RequestBody SupplierReturnRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        SupplierReturnResponse supplierReturn = supplierReturnService.create(
                request, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierReturn);
    }
}
