package com.pos.system.controller;

import com.pos.system.dto.request.SupplierRequest;
import com.pos.system.dto.response.SupplierResponse;
import com.pos.system.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de proveedores.
 */
@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@Tag(name = "Suppliers", description = "API de gestión de proveedores")
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    @PreAuthorize("hasAuthority('COMPRAS_READ')")
    @Operation(summary = "Listar todos los proveedores")
    public ResponseEntity<List<SupplierResponse>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<SupplierResponse> suppliers = activeOnly
                ? supplierService.findAllActive()
                : supplierService.findAll();
        return ResponseEntity.ok(suppliers);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COMPRAS_READ')")
    @Operation(summary = "Obtener proveedor por ID")
    public ResponseEntity<SupplierResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.findById(id));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('COMPRAS_READ')")
    @Operation(summary = "Obtener proveedor por código")
    public ResponseEntity<SupplierResponse> findByCode(@PathVariable String code) {
        return ResponseEntity.ok(supplierService.findByCode(code));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('COMPRAS_READ')")
    @Operation(summary = "Buscar proveedores por nombre")
    public ResponseEntity<List<SupplierResponse>> search(@RequestParam String term) {
        return ResponseEntity.ok(supplierService.search(term));
    }

    @GetMapping("/with-debt")
    @PreAuthorize("hasAuthority('COMPRAS_READ')")
    @Operation(summary = "Listar proveedores con deuda")
    public ResponseEntity<List<SupplierResponse>> findWithDebt() {
        return ResponseEntity.ok(supplierService.findSuppliersWithDebt());
    }

    @GetMapping("/top-rated")
    @PreAuthorize("hasAuthority('COMPRAS_READ')")
    @Operation(summary = "Listar proveedores mejor calificados")
    public ResponseEntity<List<SupplierResponse>> findTopRated() {
        return ResponseEntity.ok(supplierService.findTopRatedSuppliers());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('COMPRAS_CREATE')")
    @Operation(summary = "Crear nuevo proveedor")
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        SupplierResponse supplier = supplierService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(supplier);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('COMPRAS_UPDATE')")
    @Operation(summary = "Actualizar proveedor")
    public ResponseEntity<SupplierResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.ok(supplierService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('COMPRAS_DELETE')")
    @Operation(summary = "Eliminar proveedor (soft delete)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
