package com.pos.system.controller;

import com.pos.system.dto.request.WebhookRequest;
import com.pos.system.dto.response.WebhookDeliveryResponse;
import com.pos.system.dto.response.WebhookResponse;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.WebhookService;
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
@RequestMapping("/api/admin/webhooks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Webhooks", description = "Gestión de webhooks para API pública")
public class WebhookAdminController {

    private final WebhookService webhookService;

    @PostMapping
    @Operation(summary = "Crear webhook endpoint", description = "Retorna el secreto UNA SOLA vez")
    public ResponseEntity<WebhookResponse> create(@Valid @RequestBody WebhookRequest request,
                                                   @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(webhookService.create(request, userDetails.getId()));
    }

    @GetMapping
    @Operation(summary = "Listar todos los webhooks")
    public ResponseEntity<List<WebhookResponse>> listAll() {
        return ResponseEntity.ok(webhookService.listAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de un webhook")
    public ResponseEntity<WebhookResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(webhookService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un webhook (url, eventos)")
    public ResponseEntity<WebhookResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody WebhookRequest request) {
        return ResponseEntity.ok(webhookService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un webhook")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        webhookService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/deliveries")
    @Operation(summary = "Historial de entregas de un webhook")
    public ResponseEntity<List<WebhookDeliveryResponse>> getDeliveries(@PathVariable Long id) {
        return ResponseEntity.ok(webhookService.getDeliveries(id));
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "Enviar un payload de prueba al webhook")
    public ResponseEntity<WebhookDeliveryResponse> sendTest(@PathVariable Long id) {
        return ResponseEntity.ok(webhookService.sendTest(id));
    }
}
