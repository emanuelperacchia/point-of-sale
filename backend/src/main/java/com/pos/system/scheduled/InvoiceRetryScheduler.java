package com.pos.system.scheduled;

import com.pos.system.config.FiscalProperties;
import com.pos.system.entity.InvoiceDocument;
import com.pos.system.entity.InvoiceStatus;
import com.pos.system.repository.InvoiceRepository;
import com.pos.system.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler que reintenta la emisión de comprobantes electrónicos
 * que quedaron en estado {@link InvoiceStatus#PENDIENTE}.
 * <p>
 * Se ejecuta cada {@code fiscal.retry.interval-ms} (default: 30 segundos).
 * Solo procesa comprobantes con al menos 5 minutos de antigüedad para
 * evitar reintentar comprobantes que aún están en proceso.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class InvoiceRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(InvoiceRetryScheduler.class);

    /** Tiempo mínimo de espera antes del primer reintento. */
    private static final long MIN_AGE_FOR_RETRY_MINUTES = 5;

    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final FiscalProperties fiscalProperties;

    @Scheduled(fixedDelayString = "${fiscal.retry.interval-ms:30000}")
    public void retryPendingInvoices() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(MIN_AGE_FOR_RETRY_MINUTES);
        List<InvoiceDocument> pending = invoiceRepository
                .findByEstadoAndCreatedAtBefore(InvoiceStatus.PENDIENTE, cutoff);

        if (pending.isEmpty()) {
            return;
        }

        log.info("Reintentando {} comprobante(s) en estado PENDIENTE", pending.size());

        for (InvoiceDocument invoice : pending) {
            try {
                int intento = invoice.getIntentos() + 1;
                int maxIntentos = fiscalProperties.getRetry().getMaxAttempts();

                log.info("Reintentando comprobante {} (intento {}/{})",
                        invoice.getId(), intento, maxIntentos);

                invoiceService.retryEmission(invoice.getId());

            } catch (Exception e) {
                log.error("Error al reintentar comprobante {}: {}",
                        invoice.getId(), e.getMessage(), e);
            }
        }
    }
}
