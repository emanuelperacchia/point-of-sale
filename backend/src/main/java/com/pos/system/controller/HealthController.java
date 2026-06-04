package com.pos.system.controller;

import com.pos.system.dto.response.HealthResponse;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(name="Health Cheack", description="Endpoints para verificar estado del sistema")
public class HealthController {

    private final HealthService healthService;

    @GetMapping
    @Operation(
            summary = "Verificar estado del sistema",
            description = "Retorna el estado actual del sistema y metadata básica"
    )
    public ResponseEntity<HealthResponse> healthCheck(){
        HealthResponse response = healthService.getSystemHealth();
        return ResponseEntity.ok(response);
    }
    @GetMapping("/metrics")
    @Operation(
            summary = "Obtener métricas del sistema",
            description = "Retorna métricas de uso de recursos del sistema (memoria, etc.)"
    )
    public ResponseEntity<HealthResponse> systemMetrics() {
        HealthResponse response = healthService.getSystemMetrics();
        return ResponseEntity.ok(response);
    }
    @GetMapping("/db")
    @Operation(
            summary = "Verificar conexión a base de datos",
            description = "Prueba la conectividad con PostgreSQL"
    )

    public ResponseEntity<String> databaseCheck() {
        boolean isConnected = healthService.checkDatabaseConnection();
        String message = isConnected
                ? "Conexión a base de datos: OK ✅"
            : "Conexión a base de datos: FALLO ❌";
        return ResponseEntity.ok(message);
    }
}
