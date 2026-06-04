package com.pos.system.controller;

import com.pos.system.dto.response.HealthResponse;
import com.pos.system.service.HealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import java.time.LocalDateTime;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private HealthService healthService;
    @Test
    void healthCheck_ShouldReturnOk() throws Exception {
        // Arrange
        HealthResponse mockResponse = new HealthResponse(
                "UP",
                "Sistema funcionando correctamente",
                LocalDateTime.now(),
                "1.0.0"
        );
        when(healthService.getSystemHealth()).thenReturn(mockResponse);
        // Act & Assert
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").value("Sistema funcionando correctamente"))
                .andExpect(jsonPath("$.version").value("1.0.0"));
    }
    @Test
    void healthCheck_WhenSystemDown_ShouldReturnDownStatus() throws Exception {
        // Arrange
        HealthResponse mockResponse = new HealthResponse(
                "DOWN",
                "Error de conexión a base de datos",
                LocalDateTime.now(),
                "1.0.0"
        );
        when(healthService.getSystemHealth()).thenReturn(mockResponse);
        // Act & Assert
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.message").value("Error de conexión a base de datos"));
    }
    @Test
    void systemMetrics_ShouldReturnMetrics() throws Exception {
        // Arrange
        HealthResponse mockResponse = new HealthResponse(
                "UP",
                "Memoria: 256 MB usados de 512 MB disponibles",
                LocalDateTime.now(),
                "1.0.0"
        );
        when(healthService.getSystemMetrics()).thenReturn(mockResponse);
        // Act & Assert
        mockMvc.perform(get("/api/health/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").exists());
    }
    @Test
    void databaseCheck_WhenConnected_ShouldReturnOk() throws Exception {
        // Arrange
        when(healthService.checkDatabaseConnection()).thenReturn(true);
        // Act & Assert
        mockMvc.perform(get("/api/health/db"))
                .andExpect(status().isOk())
                .andExpect(content().string("Conexión a base de datos: OK ✅"));
    }
    @Test
    void databaseCheck_WhenDisconnected_ShouldReturnError() throws Exception {
// Arrange
        when(healthService.checkDatabaseConnection()).thenReturn(false);
// Act & Assert
        mockMvc.perform(get("/api/health/db"))
                .andExpect(status().isOk())
                .andExpect(content().string("Conexión a base de datos: FALLO ❌"));
    }
}
