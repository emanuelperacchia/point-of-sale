package com.pos.system.controller;

import com.pos.system.dto.request.RecipeRequest;
import com.pos.system.dto.response.BomExplosionResponse;
import com.pos.system.dto.response.CostEstimateResponse;
import com.pos.system.dto.response.RecipeResponse;
import com.pos.system.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','INVENTARIO')")
    public ResponseEntity<List<RecipeResponse>> listAll() {
        return ResponseEntity.ok(recipeService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','INVENTARIO')")
    public ResponseEntity<RecipeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(recipeService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<RecipeResponse> create(@Valid @RequestBody RecipeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recipeService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<RecipeResponse> update(@PathVariable Long id, @Valid @RequestBody RecipeRequest request) {
        return ResponseEntity.ok(recipeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        recipeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/bom-explosion")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','INVENTARIO')")
    public ResponseEntity<BomExplosionResponse> getBomExplosion(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") BigDecimal cantidad) {
        return ResponseEntity.ok(recipeService.getBomExplosion(id, cantidad));
    }

    @GetMapping("/{id}/cost-estimate")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','INVENTARIO')")
    public ResponseEntity<CostEstimateResponse> getCostEstimate(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") BigDecimal cantidad) {
        return ResponseEntity.ok(recipeService.getCostEstimate(id, cantidad));
    }
}
