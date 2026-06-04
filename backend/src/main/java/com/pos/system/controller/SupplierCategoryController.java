package com.pos.system.controller;

import com.pos.system.dto.request.SupplierCategoryRequest;
import com.pos.system.dto.response.SupplierCategoryResponse;
import com.pos.system.service.SupplierCategoryService;
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
 * Controlador REST para la gestión de categorías de proveedores.
 */
@RestController
@RequestMapping("/api/supplier-categories")
@RequiredArgsConstructor
@Tag(name = "Supplier Categories", description = "API de categorías de proveedores")
public class SupplierCategoryController {

    private final SupplierCategoryService categoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('COMPRAS_READ')")
    @Operation(summary = "Listar todas las categorías de proveedor")
    public ResponseEntity<List<SupplierCategoryResponse>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<SupplierCategoryResponse> categories = activeOnly
                ? categoryService.findAllActive()
                : categoryService.findAll();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COMPRAS_READ')")
    @Operation(summary = "Obtener categoría por ID")
    public ResponseEntity<SupplierCategoryResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('COMPRAS_READ')")
    @Operation(summary = "Obtener categoría por código")
    public ResponseEntity<SupplierCategoryResponse> findByCode(@PathVariable String code) {
        return ResponseEntity.ok(categoryService.findByCode(code));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('COMPRAS_CREATE')")
    @Operation(summary = "Crear nueva categoría de proveedor")
    public ResponseEntity<SupplierCategoryResponse> create(
            @Valid @RequestBody SupplierCategoryRequest request) {
        SupplierCategoryResponse category = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('COMPRAS_UPDATE')")
    @Operation(summary = "Actualizar categoría de proveedor")
    public ResponseEntity<SupplierCategoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SupplierCategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('COMPRAS_DELETE')")
    @Operation(summary = "Eliminar categoría de proveedor (soft delete)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
