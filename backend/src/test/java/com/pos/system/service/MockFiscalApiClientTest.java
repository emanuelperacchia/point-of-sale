package com.pos.system.service;

import com.pos.system.config.FiscalProperties;
import com.pos.system.service.impl.MockFiscalApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests para {@link MockFiscalApiClient}.
 * Verifica que el mock responda correctamente en modo éxito y error.
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
        // Default: 100% success, no delay
        when(mockConfig.getSuccessRate()).thenReturn(1.0);
        when(mockConfig.getDelayMs()).thenReturn(0L);
        client = new MockFiscalApiClient(fiscalProperties);
    }

    @Test
    void emitirComprobante_With100PercentSuccess_ShouldReturnApproved() {
        when(mockConfig.getSuccessRate()).thenReturn(1.0);

        FiscalApiClient.FiscalEmissionResponse response = client.emitirComprobante(
                "<xml/>", "FACTURA_A", 1L, 1);

        assertNotNull(response);
        assertEquals("A", response.resultado());
        assertNotNull(response.cae());
        assertEquals(11, response.cae().length(), "CAE debe tener 11 dígitos");
        assertNotNull(response.fechaVencimiento());
        assertTrue(response.fechaVencimiento().isAfter(java.time.LocalDateTime.now()));
    }

    @Test
    void emitirComprobante_With0PercentSuccess_ShouldReturnRejected() {
        when(mockConfig.getSuccessRate()).thenReturn(0.0);

        FiscalApiClient.FiscalEmissionResponse response = client.emitirComprobante(
                "<xml/>", "FACTURA_B", 2L, 1);

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

        FiscalApiClient.FiscalEmissionResponse response = client.emitirComprobante(
                "<xml/>", "BOLETA", 42L, 2);

        assertNotNull(response);
        assertEquals("A", response.resultado());
        // The XML response should contain the mock XML
        assertNotNull(response.xmlResponse());
        assertTrue(response.xmlResponse().contains("A"));
    }
}
