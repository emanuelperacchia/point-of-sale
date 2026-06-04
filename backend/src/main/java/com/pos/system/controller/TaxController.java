package com.pos.system.controller;

import com.pos.system.entity.Product;
import com.pos.system.entity.Tax;
import com.pos.system.service.TaxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/taxes")
@RequiredArgsConstructor
@Tag(name = "Impuestos", description = "Gestión de impuestos y tasas")
public class TaxController {

    private final TaxService taxService;

    @GetMapping
    @Operation(summary = "Listar impuestos", description = "Obtiene todos los impuestos activos")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<List<Tax>> getAllTaxes() {
        return ResponseEntity.ok(taxService.getAllActiveTaxes());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener impuesto", description = "Obtiene un impuesto por su ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<Tax> getTax(@PathVariable Long id) {
        return ResponseEntity.ok(taxService.getTaxById(id));
    }

    @PostMapping
    @Operation(summary = "Crear impuesto", description = "Crea un nuevo impuesto")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Tax> createTax(@Valid @RequestBody Tax tax) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taxService.createTax(tax));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar impuesto", description = "Actualiza un impuesto existente")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Tax> updateTax(@PathVariable Long id, @Valid @RequestBody Tax tax) {
        return ResponseEntity.ok(taxService.updateTax(id, tax));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar impuesto", description = "Desactiva un impuesto (baja lógica)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateTax(@PathVariable Long id) {
        taxService.deactivateTax(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/regions/{region}")
    @Operation(summary = "Impuestos por región", description = "Obtiene impuestos activos para una región")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<List<Tax>> getTaxesByRegion(@PathVariable String region) {
        return ResponseEntity.ok(taxService.getTaxesByRegion(region));
    }

    @PostMapping("/products/{productId}")
    @Operation(summary = "Asignar impuestos a producto", description = "Asigna impuestos a un producto")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> assignTaxesToProduct(
            @PathVariable Long productId,
            @RequestBody List<Long> taxIds) {
        return ResponseEntity.ok(taxService.assignTaxesToProduct(productId, taxIds));
    }

    @GetMapping("/products/{productId}/final-price")
    @Operation(summary = "Precio final con impuestos", description = "Calcula el precio final del producto incluyendo impuestos")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<BigDecimal> getProductFinalPrice(@PathVariable Long productId) {
        return ResponseEntity.ok(taxService.calculateProductFinalPrice(productId));
    }
}