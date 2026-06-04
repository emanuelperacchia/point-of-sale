package com.pos.system.service;

import com.pos.system.dto.response.HealthResponse;
import com.pos.system.service.impl.HealthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import javax.sql.DataSource;
import java.sql.Connection;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HeathSerciceTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;
    private HealthService healthService;
    @BeforeEach
    void setUp() {
        healthService = new HealthServiceImpl(dataSource);
    }
    @Test
    void getSystemHealth_WhenDatabaseConnected_ShouldReturnUp() throws Exception {
        // Arrange
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(anyInt())).thenReturn(true);
        // Act
        HealthResponse response = healthService.getSystemHealth();
        // Assert
        assertNotNull(response);
        assertEquals("UP", response.getStatus());
        assertEquals("Sistema funcionando correctamente", response.getMessage());
        assertNotNull(response.getTimestamp());
        assertEquals("1.0.0", response.getVersion());

        verify(dataSource, times(1)).getConnection();
    }
    @Test
    void getSystemHealth_WhenDatabaseDisconnected_ShouldReturnDown() throws Exception {
        // Arrange
        when(dataSource.getConnection()).thenThrow(new RuntimeException("DB Error"));
        // Act
        HealthResponse response = healthService.getSystemHealth();
        // Assert
        assertNotNull(response);
        assertEquals("DOWN", response.getStatus());
        assertEquals("Error de conexión a base de datos", response.getMessage());
    }
    @Test
    void checkDatabaseConnection_WhenConnectionValid_ShouldReturnTrue() throws Exception {
        // Arrange
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(anyInt())).thenReturn(true);
        // Act
        boolean result = healthService.checkDatabaseConnection();
        // Assert
        assertTrue(result);
        verify(connection).isValid(2);
    }
    @Test
    void checkDatabaseConnection_WhenConnectionInvalid_ShouldReturnFalse() throws Exception {
        // Arrange
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(anyInt())).thenReturn(false);
        // Act
        boolean result = healthService.checkDatabaseConnection();
        // Assert
        assertFalse(result);
    }

    @Test
    void checkDatabaseConnection_WhenExceptionThrown_ShouldReturnFalse() throws Exception {
        // Arrange
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection failed"));
        // Act
        boolean result = healthService.checkDatabaseConnection();
        // Assert
        assertFalse(result);
    }
    @Test
    void getSystemMetrics_ShouldReturnMemoryInfo() {
        // Act
        HealthResponse response = healthService.getSystemMetrics();
        // Assert
        assertNotNull(response);
        assertEquals("UP", response.getStatus());
        assertTrue(response.getMessage().contains("Memoria"));
        assertTrue(response.getMessage().contains("MB"));
        assertNotNull(response.getTimestamp());
    }
}
