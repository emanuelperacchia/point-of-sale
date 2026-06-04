package com.pos.system.service.impl;

import com.pos.system.config.FiscalProperties;
import com.pos.system.service.FiscalApiClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * Implementación mock del cliente de API fiscal.
 * <p>
 * Activa por defecto ({@code fiscal.retry.mock=true}) o cuando {@code fiscal.mode=mock}.
 * Simula el envío de comprobantes con una tasa de éxito configurable.
 * </p>
 */
@Service
@ConditionalOnProperty(name = "fiscal.mode", havingValue = "mock", matchIfMissing = true)
@RequiredArgsConstructor
public class MockFiscalApiClient implements FiscalApiClient {

    private static final Logger log = LoggerFactory.getLogger(MockFiscalApiClient.class);

    private final FiscalProperties properties;
    private final Random random = new Random();

    @Override
    public FiscalEmissionResponse emitirComprobante(
            String xmlFirmado,
            String tipoComprobante,
            Long numero,
            Integer puntoVenta) {

        log.info("[MOCK] Emitiendo comprobante {} - Tipo: {}, Nro: {}, PV: {}",
                tipoComprobante, numero, puntoVenta);

        // Simular latencia de red
        if (properties.getMock().getDelayMs() > 0) {
            try {
                Thread.sleep(properties.getMock().getDelayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Simular éxito/fallo según tasa configurada
        boolean success = random.nextDouble() < properties.getMock().getSuccessRate();

        if (success) {
            // Generar CAE simulado: 11 dígitos
            String cae = String.format("%011d",
                    Math.abs(random.nextLong() % 100_000_000_000L));
            LocalDateTime vencimiento = LocalDateTime.now().plusDays(15);

            log.info("[MOCK] Comprobante {} APROBADO - CAE: {}", numero, cae);

            return new FiscalEmissionResponse(
                    cae,
                    vencimiento,
                    "A",
                    "Comprobante autorizado",
                    "<respuesta><resultado>A</resultado>"
                            + "<CAE>" + cae + "</CAE>"
                            + "<vencimiento>" + vencimiento + "</vencimiento>"
                            + "</respuesta>"
            );
        } else {
            log.warn("[MOCK] Comprobante {} RECHAZADO (simulado)", numero);

            return new FiscalEmissionResponse(
                    null,
                    null,
                    "R",
                    "Rechazo simulado: el comprobante no cumple con las validaciones fiscales",
                    "<respuesta><resultado>R</resultado>"
                            + "<observaciones>Rechazo simulado</observaciones>"
                            + "</respuesta>"
            );
        }
    }
}
