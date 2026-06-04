package com.pos.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades de configuración para el módulo de facturación electrónica.
 * <p>
 * Se mapea desde {@code fiscal.*} en application.yml.
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "fiscal")
@Data
public class FiscalProperties {

    /**
     * Modo de operación: {@code mock} (por defecto), {@code homologacion}, {@code produccion}.
     */
    private String mode = "mock";

    /** Configuración de reintentos para emisiones fallidas. */
    private Retry retry = new Retry();

    /** Configuración del mock (solo aplica cuando mode=mock). */
    private Mock mock = new Mock();

    /** Configuración de generación de PDF. */
    private Pdf pdf = new Pdf();

    @Data
    public static class Retry {
        /** Número máximo de reintentos antes de marcar como RECHAZADO. */
        private int maxAttempts = 3;

        /** Intervalo entre ejecuciones del scheduler de reintentos (ms). */
        private long intervalMs = 30_000;
    }

    @Data
    public static class Mock {
        /** Probabilidad de éxito (0.0 - 1.0). 0.95 = 95% de éxito. */
        private double successRate = 0.95;

        /** Delay simulado de red (ms). */
        private long delayMs = 300;
    }

    @Data
    public static class Pdf {
        /** Directorio donde se guardan los PDFs generados. */
        private String outputDir = "./invoices";
    }
}
