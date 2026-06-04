package com.pos.system.controller;

import com.pos.system.dto.request.ProductRequest;
import com.pos.system.dto.response.ProductResponse;
import com.pos.system.dto.response.ProductSearchResponse;
import com.pos.system.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Productos", description = "Gestión de productos")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'INVENTARIO')")
    @Operation(summary = "Crear producto", description = "Crea un nuevo producto en el sistema")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @GetMapping
    @Operation(summary = "Listar productos", description = "Obtiene todos los productos con paginación")
    public ResponseEntity<Page<ProductResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<ProductResponse> products;
        if (search != null && !search.isBlank()) {
            products = productService.search(search, pageable);
        } else {
            products = productService.getAll(pageable);
        }
        
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por ID", description = "Retorna un producto específico")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'INVENTARIO')")
    @Operation(summary = "Actualizar producto", description = "Actualiza un producto existente")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Eliminar producto", description = "Elimina un producto (soft delete)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'INVENTARIO')")
    @Operation(summary = "Actualizar stock", description = "Actualiza el stock de un producto")
    public ResponseEntity<Void> updateStock(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        productService.updateStock(id, quantity);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO', 'VENDEDOR', 'INVENTARIO')")
    @Operation(summary = "Buscar productos para POS",
              description = "Búsqueda rápida por nombre, SKU o categoría para el punto de venta")
    public ResponseEntity<List<ProductSearchResponse>> searchForPos(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(productService.searchForPos(q, limit));
    }
}
