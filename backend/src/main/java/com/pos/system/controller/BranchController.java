package com.pos.system.controller;

import com.pos.system.entity.Branch;
import com.pos.system.service.BranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
@Tag(name = "Sucursales", description = "Gestión de sucursales multi-tienda")
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    @Operation(summary = "Listar sucursales activas")
    public ResponseEntity<List<Branch>> listActive() {
        return ResponseEntity.ok(branchService.findActive());
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todas las sucursales (ADMIN)")
    public ResponseEntity<List<Branch>> listAll() {
        return ResponseEntity.ok(branchService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener sucursal por ID")
    public ResponseEntity<Branch> getById(@PathVariable Long id) {
        return ResponseEntity.ok(branchService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear sucursal (ADMIN)")
    public ResponseEntity<Branch> create(@Valid @RequestBody Branch branch) {
        return ResponseEntity.ok(branchService.create(branch));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar sucursal (ADMIN)")
    public ResponseEntity<Branch> update(@PathVariable Long id, @Valid @RequestBody Branch branch) {
        return ResponseEntity.ok(branchService.update(id, branch));
    }

    @PostMapping("/{branchId}/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Asignar usuario a sucursal (ADMIN)")
    public ResponseEntity<Void> assignUser(@PathVariable Long branchId, @PathVariable Long userId) {
        branchService.assignUser(userId, branchId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{branchId}/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remover usuario de sucursal (ADMIN)")
    public ResponseEntity<Void> removeUser(@PathVariable Long branchId, @PathVariable Long userId) {
        branchService.removeUser(userId, branchId);
        return ResponseEntity.ok().build();
    }
}
