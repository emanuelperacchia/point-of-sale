package com.pos.system.controller;

import com.pos.system.dto.request.ConfigUpdateRequest;
import com.pos.system.entity.SystemConfiguration;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.ConfigurationService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@Tag(name = "Configuraciones", description = "Parámetros configurables del sistema")
public class ConfigurationController {

    private final ConfigurationService configurationService;

    @GetMapping
    @Operation(summary = "Todas las configuraciones", description = "Obtiene todas las configuraciones activas")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<SystemConfiguration>> getAllConfigs() {
        return ResponseEntity.ok(configurationService.getAllConfigs());
    }

    @GetMapping("/map")
    @Operation(summary = "Configuraciones como mapa", description = "Obtiene todas las configuraciones como clave-valor")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<Map<String, String>> getConfigMap() {
        return ResponseEntity.ok(configurationService.getConfigMap());
    }

    @GetMapping("/groups/{groupName}")
    @Operation(summary = "Configuraciones por grupo", description = "Filtra configuraciones por grupo")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<SystemConfiguration>> getConfigsByGroup(
            @PathVariable String groupName) {
        return ResponseEntity.ok(configurationService.getConfigsByGroup(groupName));
    }

    @GetMapping("/{configKey}")
    @Operation(summary = "Obtener configuración", description = "Obtiene una configuración por su key")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<SystemConfiguration> getConfig(
            @PathVariable String configKey) {
        return ResponseEntity.ok(configurationService.getConfig(configKey));
    }

    @PutMapping("/{configKey}")
    @Operation(summary = "Actualizar configuración", description = "Actualiza el valor de una configuración")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemConfiguration> updateConfig(
            @PathVariable String configKey,
            @Valid @RequestBody ConfigUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(
                configurationService.updateConfig(configKey, request.getConfigValue(), userDetails.getUser()));
    }

    @PostMapping
    @Operation(summary = "Crear configuración", description = "Crea una nueva configuración")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemConfiguration> createConfig(
            @Valid @RequestBody SystemConfiguration config) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(configurationService.createConfig(config));
    }
}