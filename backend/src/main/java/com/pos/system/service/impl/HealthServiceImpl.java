package com.pos.system.service.impl;

import com.pos.system.dto.response.HealthResponse;
import com.pos.system.service.HealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthServiceImpl implements HealthService {
    private final DataSource dataSource;
    @Override
    public HealthResponse getSystemHealth() {
        log.debug("Getting system health status");

        boolean dbConnected = checkDatabaseConnection();
        String status = dbConnected ? "UP" : "DOWN";
        String message = dbConnected
                ? "Sistema funcionando correctamente"
                : "Error de conexión a base de datos";
        return new HealthResponse(
                status,
                message,
                LocalDateTime.now(),
                "1.0.0"
        );
    }
    @Override
    public boolean checkDatabaseConnection() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2); // timeout 2 segundos
        } catch (Exception e) {
            log.error("Error checking database connection", e);
            return false;
        }
    }

    @Override
    public HealthResponse getSystemMetrics() {
        log.debug("Getting system metrics");

        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        String message = String.format(
                "Memoria: %d MB usados de %d MB disponibles",
                usedMemory / (1024 * 1024),
                totalMemory / (1024 * 1024)
        );
        return new HealthResponse(
                "UP",
                message,
                LocalDateTime.now(),
                "1.0.0"
        );
    }
}