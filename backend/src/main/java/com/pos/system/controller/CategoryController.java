package com.pos.system.controller;

import com.pos.system.dto.request.CategoryRequest;
import com.pos.system.dto.response.CategoryResponse;
import com.pos.system.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categorías", description = "Gestión de categorías de productos")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'INVENTARIO')")
    @Operation(summary = "Crear categoría", description = "Crea una nueva categoría")
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    @GetMapping
    @Operation(summary = "Listar categorías", description = "Obtiene todas las categorías con paginación")
    public ResponseEntity<Page<CategoryResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(categoryService.getAll(pageable));
    }

    @GetMapping("/active")
    @Operation(summary = "Listar categorías activas", description = "Obtiene solo categorías activas para selects")
    public ResponseEntity<List<CategoryResponse>> getAllActive() {
        return ResponseEntity.ok(categoryService.getAllActive());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener categoría por ID")
    public ResponseEntity<CategoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'INVENTARIO')")
    @Operation(summary = "Actualizar categoría")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Eliminar categoría")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
