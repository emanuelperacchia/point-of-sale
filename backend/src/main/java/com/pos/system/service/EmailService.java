package com.pos.system.service;

import com.pos.system.entity.InvoiceDocument;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * Servicio de envío de correos electrónicos para comprobantes.
 * <p>
 * Deshabilitado por defecto ({@code fiscal.email.enabled=false}).
 * El {@link JavaMailSender} se inyecta de forma opcional mediante
 * {@link ObjectProvider}, permitiendo que la aplicación funcione
 * incluso sin configuración SMTP.
 * </p>
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final boolean mailAvailable;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                        @Value("${fiscal.email.enabled:false}") boolean enabled) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.mailAvailable = this.mailSender != null;
        this.enabled = enabled;
    }

    /**
     * Envía el comprobante por email al receptor.
     * <p>
     * Si el servicio de email está deshabilitado o no hay configuración SMTP,
     * solo registra en log sin enviar.
     * </p>
     *
     * @param invoice    comprobante emitido
     * @param pdfPath    ruta del archivo PDF
     * @param toAddress  dirección de correo del receptor
     */
    public void sendInvoiceEmail(InvoiceDocument invoice, String pdfPath, String toAddress) {
        if (!enabled) {
            log.info("[EMAIL DESHABILITADO] Se enviaría comprobante {} a {} (PDF: {})",
                    invoice.getNumero(), toAddress, pdfPath);
            return;
        }

        if (!mailAvailable) {
            log.warn("[EMAIL NO CONFIGURADO] No hay JavaMailSender disponible. " +
                     "Configure spring.mail.* para enviar emails. " +
                     "Comprobante: {}, destinatario: {}", invoice.getNumero(), toAddress);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toAddress);
            helper.setSubject(createSubject(invoice));
            helper.setText(createBody(invoice));

            File pdfFile = new File(pdfPath);
            if (pdfFile.exists()) {
                helper.addAttachment("comprobante.pdf", new FileSystemResource(pdfFile));
            }

            mailSender.send(message);
            log.info("Email enviado a {} para comprobante {}", toAddress, invoice.getNumero());

        } catch (MessagingException e) {
            log.error("Error al enviar email del comprobante {} a {}: {}",
                    invoice.getNumero(), toAddress, e.getMessage());
        }
    }

    private String createSubject(InvoiceDocument invoice) {
        return String.format("Comprobante %s Nro %04d-%08d",
                invoice.getTipoComprobante().name().replace("_", " "),
                invoice.getPuntoVenta(),
                invoice.getNumero());
    }

    private String createBody(InvoiceDocument invoice) {
        return String.format("""
                Estimado cliente,
                
                Adjuntamos el comprobante electrónico:
                
                Tipo: %s
                Número: %04d-%08d
                CAE: %s
                Fecha: %s
                
                Gracias por su compra.
                """,
                invoice.getTipoComprobante().name().replace("_", " "),
                invoice.getPuntoVenta(),
                invoice.getNumero(),
                invoice.getCae() != null ? invoice.getCae() : "—",
                invoice.getCreatedAt() != null ? invoice.getCreatedAt().toLocalDate() : "—");
    }
}
