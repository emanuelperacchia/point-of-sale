package com.pos.system.service.impl;

import com.pos.system.config.FiscalProperties;
import com.pos.system.dto.response.InvoiceResponse;
import com.pos.system.entity.*;
import com.pos.system.event.SaleCompletedEvent;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.*;
import com.pos.system.service.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de facturación electrónica.
 * <p>
 * Escucha el evento {@link SaleCompletedEvent} para emitir automáticamente
 * el comprobante después de cada venta completada.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceServiceImpl.class);

    private final SaleRepository saleRepository;
    private final InvoiceRepository invoiceRepository;
    private final CompanyRepository companyRepository;
    private final DigitalCertificateRepository certificateRepository;
    private final XmlBuilderService xmlBuilder;
    private final DigitalSignatureService digitalSignatureService;
    private final FiscalApiClient fiscalApiClient;
    private final FiscalProperties fiscalProperties;
    private final PdfGeneratorService pdfGenerator;
    private final QrCodeService qrCodeService;

    // ──────────────────────────────────────────────
    // Event Listener
    // ──────────────────────────────────────────────

    /**
     * Escucha el evento de venta completada y emite el comprobante.
     * <p>
     * Se ejecuta en una transacción separada ({@code REQUIRES_NEW}) para
     * que el fallo en la emisión no afecte la transacción de la venta.
     * </p>
     */
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleSaleCompleted(SaleCompletedEvent event) {
        log.info("Evento recibido: venta {} completada — emitiendo comprobante", event.saleId());
        try {
            InvoiceResponse response = emitInvoice(event.saleId());
            if (response.estado() == InvoiceStatus.EMITIDO) {
                generateInvoiceDeliverables(response.id());
            }
        } catch (Exception e) {
            log.error("Error al emitir comprobante para venta {}: {}", event.saleId(), e.getMessage(), e);
        }
    }

    // ──────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────

    @Override
    @Transactional
    public InvoiceResponse emitInvoice(Long saleId) {
        // Idempotencia: si ya hay comprobante, devolverlo
        Optional<InvoiceDocument> existing = invoiceRepository.findBySaleId(saleId);
        if (existing.isPresent()) {
            log.info("Comprobante ya existe para venta {}, retornando existente", saleId);
            return mapToResponse(existing.get());
        }

        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada: " + saleId));

        Client client = sale.getClient();
        Company company = companyRepository.findByActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay empresa activa configurada. " +
                        "Registre los datos fiscales en /api/companies."));

        // Determinar tipo de comprobante según condición fiscal del cliente
        TipoComprobante tipo = determineTipoComprobante(client);

        // Obtener próximo número correlativo desde la sequence PostgreSQL
        // (thread-safe, sin race condition como el anterior SELECT MAX + 1)
        Long numero = invoiceRepository.nextSequenceValue(tipo.name());

        // 1. Generar XML
        String xml = xmlBuilder.buildInvoiceXml(sale, client, company, tipo, numero);
        log.debug("XML generado para comprobante {} ({} bytes)", numero, xml.length());

        // 2. Firmar XML (si hay certificado activo)
        String xmlFirmado;
        DigitalCertificate cert = certificateRepository.findByActiveTrue().orElse(null);
        if (cert != null) {
            xmlFirmado = digitalSignatureService.signXml(xml, cert);
            log.debug("XML firmado con certificado: {}", cert.getAlias());
        } else {
            xmlFirmado = xml;
            log.warn("No hay certificado digital activo — XML enviado sin firma");
        }

        // 3. Enviar al organismo fiscal
        FiscalApiClient.FiscalEmissionResponse fiscalResponse = fiscalApiClient.emitirComprobante(
                xmlFirmado, tipo.name(), numero, company.getPuntoVenta());

        // 4. Persistir comprobante
        InvoiceDocument invoice = InvoiceDocument.builder()
                .saleId(sale.getId())
                .tipoComprobante(tipo)
                .puntoVenta(company.getPuntoVenta())
                .numero(numero)
                .cae("A".equals(fiscalResponse.resultado()) ? fiscalResponse.cae() : null)
                .fechaCae("A".equals(fiscalResponse.resultado()) ? fiscalResponse.fechaVencimiento() : null)
                .xmlFirmado(xmlFirmado)
                .estado("A".equals(fiscalResponse.resultado()) ? InvoiceStatus.EMITIDO : InvoiceStatus.RECHAZADO)
                .intentos(1)
                .motivoRechazo(!"A".equals(fiscalResponse.resultado()) ? fiscalResponse.observaciones() : null)
                .receptorNombre(client != null
                        ? (client.getBusinessName() != null ? client.getBusinessName() : client.getName())
                        : "Consumidor Final")
                .receptorDocumento(client != null ? client.getDocumentNumber() : null)
                .receptorCondicionIva(client != null && client.getCondicionIva() != null
                        ? client.getCondicionIva()
                        : CondicionIva.CONSUMIDOR_FINAL)
                .build();

        invoice = invoiceRepository.save(invoice);

        log.info("Comprobante {} emitido: {} (CAE: {}, tipo: {})",
                numero, invoice.getEstado(), invoice.getCae(), tipo);

        return mapToResponse(invoice);
    }

    @Override
    @Transactional
    public InvoiceResponse retryEmission(Long invoiceId) {
        InvoiceDocument invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Comprobante no encontrado: " + invoiceId));

        if (invoice.getEstado() != InvoiceStatus.PENDIENTE) {
            throw new BadRequestException(
                    "Solo se pueden reintentar comprobantes en estado PENDIENTE. " +
                    "Estado actual: " + invoice.getEstado());
        }

        int nuevoIntento = invoice.getIntentos() + 1;

        if (nuevoIntento > fiscalProperties.getRetry().getMaxAttempts()) {
            invoice.setEstado(InvoiceStatus.RECHAZADO);
            invoice.setMotivoRechazo("Máximo de reintentos alcanzado ("
                    + fiscalProperties.getRetry().getMaxAttempts() + ")");
            invoice.setIntentos(nuevoIntento);
            invoice = invoiceRepository.save(invoice);
            log.warn("Comprobante {} RECHAZADO tras {} reintentos", invoice.getId(), nuevoIntento - 1);
            return mapToResponse(invoice);
        }

        invoice.setIntentos(nuevoIntento);

        Company company = companyRepository.findByActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay empresa activa configurada"));

        FiscalApiClient.FiscalEmissionResponse fiscalResponse = fiscalApiClient.emitirComprobante(
                invoice.getXmlFirmado(),
                invoice.getTipoComprobante().name(),
                invoice.getNumero(),
                invoice.getPuntoVenta());

        if ("A".equals(fiscalResponse.resultado())) {
            invoice.setEstado(InvoiceStatus.EMITIDO);
            invoice.setCae(fiscalResponse.cae());
            invoice.setFechaCae(fiscalResponse.fechaVencimiento());
            invoice.setMotivoRechazo(null);
            log.info("Comprobante {} emitido exitosamente en reintento {}", invoice.getNumero(), nuevoIntento);
        } else {
            invoice.setMotivoRechazo(fiscalResponse.observaciones());
            log.warn("Comprobante {} aún rechazado en intento {}: {}",
                    invoice.getNumero(), nuevoIntento, fiscalResponse.observaciones());
        }

        invoice = invoiceRepository.save(invoice);
        return mapToResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getById(Long id) {
        InvoiceDocument invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Comprobante no encontrado: " + id));
        return mapToResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> findByFilters(
            TipoComprobante tipo, InvoiceStatus estado,
            LocalDateTime desde, LocalDateTime hasta, Long saleId) {
        return invoiceRepository.findByFilters(tipo, estado, desde, hasta, saleId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────
    // PDF / QR / Email
    // ──────────────────────────────────────────────

    /**
     * Genera los entregables del comprobante (PDF + QR) y actualiza el registro.
     * <p>
     * Se ejecuta fuera de la transacción principal para no bloquear recursos de I/O.
     * </p>
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void generateInvoiceDeliverables(Long invoiceId) {
        try {
            InvoiceDocument invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Comprobante no encontrado: " + invoiceId));

            Sale sale = saleRepository.findById(invoice.getSaleId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Venta no encontrada: " + invoice.getSaleId()));

            Company company = companyRepository.findByActiveTrue()
                    .orElse(null);

            if (company == null) {
                log.warn("No hay empresa activa — no se generará PDF para comprobante {}", invoiceId);
                return;
            }

            // Generar PDF
            String pdfPath = pdfGenerator.generateInvoicePdf(invoice, sale, company);

            // Generar QR (base64)
            String qrBase64 = qrCodeService.generateQrBase64(invoice);

            // Actualizar invoice con las rutas (en una transacción propia)
            invoice.setPdfPath(pdfPath);
            invoice.setQrCode(qrBase64);
            invoiceRepository.save(invoice);

            log.info("Entregables generados para comprobante {}: PDF={}, QR={} bytes",
                    invoice.getNumero(), pdfPath, qrBase64.length());

        } catch (Exception e) {
            log.error("Error al generar entregables para comprobante {}: {}",
                    invoiceId, e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Resource getPdf(Long invoiceId) {
        InvoiceDocument invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Comprobante no encontrado: " + invoiceId));

        if (invoice.getPdfPath() == null) {
            throw new BadRequestException(
                    "El comprobante aún no tiene PDF generado");
        }

        Path file = Path.of(invoice.getPdfPath());
        if (!Files.exists(file)) {
            throw new ResourceNotFoundException(
                    "Archivo PDF no encontrado: " + invoice.getPdfPath());
        }

        return new FileSystemResource(file.toFile());
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    /**
     * Determina el tipo de comprobante según la condición fiscal del cliente.
     * <p>
     * Mapeo estándar:
     * <ul>
     *   <li>Responsable Inscripto → Factura A</li>
     *   <li>Monotributista → Factura C</li>
     *   <li>Exento → Factura B</li>
     *   <li>Consumidor Final / Sin cliente → Boleta</li>
     * </ul>
     * </p>
     */
    private TipoComprobante determineTipoComprobante(Client client) {
        if (client == null || client.getCondicionIva() == null) {
            return TipoComprobante.BOLETA;
        }
        return switch (client.getCondicionIva()) {
            case RESPONSABLE_INSCRIPTO -> TipoComprobante.FACTURA_A;
            case MONOTRIBUTISTA -> TipoComprobante.FACTURA_C;
            case EXENTO -> TipoComprobante.FACTURA_B;
            case CONSUMIDOR_FINAL -> TipoComprobante.BOLETA;
        };
    }

    private InvoiceResponse mapToResponse(InvoiceDocument doc) {
        return InvoiceResponse.builder()
                .id(doc.getId())
                .saleId(doc.getSaleId())
                .tipoComprobante(doc.getTipoComprobante())
                .puntoVenta(doc.getPuntoVenta())
                .numero(doc.getNumero())
                .cae(doc.getCae())
                .fechaCae(doc.getFechaCae())
                .estado(doc.getEstado())
                .motivoRechazo(doc.getMotivoRechazo())
                .receptorNombre(doc.getReceptorNombre())
                .receptorDocumento(doc.getReceptorDocumento())
                .receptorCondicionIva(doc.getReceptorCondicionIva())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
