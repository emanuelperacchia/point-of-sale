package com.pos.system.service;

import com.pos.system.config.FiscalProperties;
import com.pos.system.service.impl.MockFiscalApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests para {@link MockFiscalApiClient}.
 * Verifica que el mock responda correctamente en modo éxito y error
 * usando el nuevo contrato estructurado {@link FiscalEmissionRequest}.
 */
@ExtendWith(MockitoExtension.class)
class MockFiscalApiClientTest {

    @Mock
    private FiscalProperties fiscalProperties;

    @Mock
    private FiscalProperties.Mock mockConfig;

    private MockFiscalApiClient client;

    @BeforeEach
    void setUp() {
        when(fiscalProperties.getMock()).thenReturn(mockConfig);
        // Default: 100% éxito, sin delay
        when(mockConfig.getSuccessRate()).thenReturn(1.0);
        when(mockConfig.getDelayMs()).thenReturn(0L);
        client = new MockFiscalApiClient(fiscalProperties);
    }

    private FiscalApiClient.FiscalEmissionRequest request() {
        return new FiscalApiClient.FiscalEmissionRequest(
                1, null, 1L, LocalDateTime.now(),
                1, 96L, "12345678", "Juan Pérez", "Av. Falsa 123", null,
                BigDecimal.valueOf(3000), BigDecimal.ZERO, BigDecimal.valueOf(3000),
                BigDecimal.ZERO, BigDecimal.ZERO, "PES", 1.0, List.of()
        );
    }

    @Test
    void emitirComprobante_With100PercentSuccess_ShouldReturnApproved() {
        when(mockConfig.getSuccessRate()).thenReturn(1.0);

        FiscalApiClient.FiscalEmissionResponse response = client.emitirComprobante(request());

        assertNotNull(response);
        assertEquals("A", response.resultado());
        assertNotNull(response.cae());
        assertEquals(11, response.cae().length(), "CAE debe tener 11 dígitos");
        assertNotNull(response.fechaVencimiento());
        assertTrue(response.fechaVencimiento().isAfter(LocalDateTime.now()));
    }

    @Test
    void emitirComprobante_With0PercentSuccess_ShouldReturnRejected() {
        when(mockConfig.getSuccessRate()).thenReturn(0.0);

        FiscalApiClient.FiscalEmissionResponse response = client.emitirComprobante(request());

        assertNotNull(response);
        assertEquals("R", response.resultado());
        assertNull(response.cae());
        assertNull(response.fechaVencimiento());
        assertNotNull(response.observaciones());
        assertTrue(response.observaciones().contains("Rechazo simulado"));
    }

    @Test
    void emitirComprobante_ShouldReturnCorrectTipoAndNumero() {
        when(mockConfig.getSuccessRate()).thenReturn(1.0);

        FiscalApiClient.FiscalEmissionResponse response = client.emitirComprobante(request());

        assertNotNull(response);
        assertEquals("A", response.resultado());
        assertNotNull(response.xmlResponse());
        assertTrue(response.xmlResponse().contains("A"));
    }
}
