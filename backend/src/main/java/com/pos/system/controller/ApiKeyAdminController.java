package com.pos.system.controller;

import com.pos.system.dto.request.ApiKeyRequest;
import com.pos.system.dto.response.ApiKeyResponse;
import com.pos.system.dto.response.ApiKeyUsageResponse;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.ApiKeyService;
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

@RestController
@RequestMapping("/api/admin/api-keys")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - API Keys", description = "Gestión de claves de API pública")
public class ApiKeyAdminController {

    private final ApiKeyService apiKeyService;

    @PostMapping
    @Operation(summary = "Crear nueva API key", description = "Retorna la clave UNA SOLA vez")
    public ResponseEntity<ApiKeyResponse> create(@Valid @RequestBody ApiKeyRequest request,
                                                   @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiKeyService.create(request, userDetails.getId()));
    }

    @GetMapping
    @Operation(summary = "Listar todas las API keys")
    public ResponseEntity<List<ApiKeyResponse>> listAll() {
        return ResponseEntity.ok(apiKeyService.listAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de una API key")
    public ResponseEntity<ApiKeyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(apiKeyService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una API key (nombre, permisos, rateLimit)")
    public ResponseEntity<ApiKeyResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody ApiKeyRequest request) {
        return ResponseEntity.ok(apiKeyService.update(id, request));
    }

    @PostMapping("/{id}/revoke")
    @Operation(summary = "Revocar una API key (desactivar)")
    public ResponseEntity<Void> revoke(@PathVariable Long id) {
        apiKeyService.revoke(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/usage")
    @Operation(summary = "Obtener estadísticas de uso de una API key")
    public ResponseEntity<ApiKeyUsageResponse> getUsage(@PathVariable Long id) {
        return ResponseEntity.ok(apiKeyService.getUsage(id));
    }
}
