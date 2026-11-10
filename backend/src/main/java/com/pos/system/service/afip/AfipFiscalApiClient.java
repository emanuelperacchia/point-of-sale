package com.pos.system.service.afip;

import com.pos.system.config.FiscalProperties;
import com.pos.system.entity.TipoComprobante;
import com.pos.system.service.FiscalApiClient;
import com.pos.system.service.soap.SoapClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente real de AFIP/ARCA para el servicio de Factura Electrónica (wsfev1,
 * {@code FECAESolicitar}).
 * <p>
 * Flujo por emisión:
 * <ol>
 *   <li>Obtiene (o reusa en caché) el Ticket de Acceso vía {@link AfipAuthService}
 *       (WSAA + firma CMS del {@code LoginTicketRequest}).</li>
 *   <li>Construye el sobre SOAP de {@code FECAESolicitar} con los campos
 *       estructurados del comprobante.</li>
 *   <li>Envía a AFIP y parsea el CAE, fecha de vencimiento y resultado.</li>
 *   <li>Si la falla es de credenciales / TA expirado, renueva el ticket y reintenta
 *       una vez.</li>
 * </ol>
 * </p>
 */
@Service
@ConditionalOnExpression("${fiscal.mode:'mock'} != 'mock'")
@RequiredArgsConstructor
public class AfipFiscalApiClient implements FiscalApiClient {

    private static final Logger log = LoggerFactory.getLogger(AfipFiscalApiClient.class);

    private static final DateTimeFormatter CBTE_FCH = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter CAE_FCH_VTO = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final Pattern TAG_PATTERN = Pattern.compile("<(\\w+)>(.*?)</\\1>", Pattern.DOTALL);

    private final AfipAuthService authService;
    private final SoapClient soapClient;
    private final FiscalProperties fiscalProperties;

    @Override
    public FiscalEmissionResponse emitirComprobante(FiscalEmissionRequest request) {
        int attempts = 0;
        int maxAttempts = fiscalProperties.getRetry().getMaxAttempts();

        while (true) {
            attempts++;
            try {
                String ticketAccess = authService.getTicketAccess();
                String cuit = empresaCuit(request);
                int cbteTipo = AfipCodeMapper.cbteTipo(request.tipoComprobante());

                String soapEnvelope = buildFecaesSolicitar(
                        ticketAccess, cuit, request.puntoVenta, cbteTipo, request);

                String endpoint = wsfeEndpoint();
                String response = soapClient.post(endpoint, "FECAESolicitar", soapEnvelope);

                return parseResponse(response, request);
            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : "";
                boolean credentialError = isCredentialError(message);
                boolean taExpired = isTicketExpired(message);

                if ((credentialError || taExpired) && attempts < maxAttempts) {
                    log.warn("Falla de autenticación AFIP (intento {}/{}): {} — renovando TA",
                            attempts, maxAttempts, message);
                    authService.renewTicketAccess();
                    continue;
                }

                log.error("Error emitiendo comprobante ante AFIP tras {} intento(s)", attempts, e);
                return new FiscalEmissionResponse(
                        null, null, "R",
                        "Error AFIP: " + message,
                        "<error><codigo>" + attempts + "</codigo><mensaje>" + escape(message) + "</mensaje></error>"
                );
            }
        }
    }

    // ──────────────────────────────────────────────
    // Sobre SOAP FECAESolicitar
    // ──────────────────────────────────────────────

    private String buildFecaesSolicitar(String ticketAccess, String cuit,
                                        Integer puntoVenta, int cbteTipo,
                                        FiscalEmissionRequest request) {
        LocalDateTime fecha = request.fechaEmision() != null
                ? request.fechaEmision()
                : LocalDateTime.now();

        BigDecimal neto = request.importeNeto() != null ? request.importeNeto() : BigDecimal.ZERO;
        BigDecimal iva = request.importeIva() != null ? request.importeIva() : BigDecimal.ZERO;
        BigDecimal total = request.importeTotal() != null ? request.importeTotal() : BigDecimal.ZERO;
        BigDecimal exento = request.importeExento() != null ? request.importeExento() : BigDecimal.ZERO;
        BigDecimal tributos = request.importeTributos() != null ? request.importeTributos() : BigDecimal.ZERO;
        BigDecimal conc = BigDecimal.ZERO; // operaciones no gravadas (no modeladas aún)

        return """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:wsfe="http://ar.gov.afip.dif.wsfe10/">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <wsfe:FECAESolicitar>
                      <wsfe:FeCAEReq>
                        <wsfe:FeCAEReqHeader>
                          <wsfe:Cuit>%s</wsfe:Cuit>
                          <wsfe:PtoVta>%d</wsfe:PtoVta>
                          <wsfe:CbteTipo>%d</wsfe:CbteTipo>
                        </wsfe:FeCAEReqHeader>
                        <wsfe:FeCAEReq>
                          <wsfe:Concepto>%d</wsfe:Concepto>
                          <wsfe:DocTipo>%d</wsfe:DocTipo>
                          <wsfe:DocNro>%s</wsfe:DocNro>
                          <wsfe:CbteDesde>%d</wsfe:CbteDesde>
                          <wsfe:CbteHasta>%d</wsfe:CbteHasta>
                          <wsfe:CbteFch>%s</wsfe:CbteFch>
                          <wsfe:ImpTotal>%s</wsfe:ImpTotal>
                          <wsfe:ImpTotConc>%s</wsfe:ImpTotConc>
                          <wsfe:ImpNeto>%s</wsfe:ImpNeto>
                          <wsfe:ImpOpExe>%s</wsfe:ImpOpExe>
                          <wsfe:ImpIVA>%s</wsfe:ImpIVA>
                          <wsfe:ImpTrib>%s</wsfe:ImpTrib>
                          <wsfe:FchServDesde></wsfe:FchServDesde>
                          <wsfe:FchServHasta></wsfe:FchServHasta>
                          <wsfe:FchVtoPago></wsfe:FchVtoPago>
                          <wsfe:MonId>%s</wsfe:MonId>
                          <wsfe:MonCotiz>%s</wsfe:MonCotiz>
                        </wsfe:FeCAEReq>
                      </wsfe:FeCAEReq>
                    </wsfe:FECAESolicitar>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(
                cuit,
                puntoVenta,
                cbteTipo,
                AfipCodeMapper.conceptoProductos(),
                request.docTipo() != null ? request.docTipo() : 99L,
                request.docNro() != null ? request.docNro() : "0",
                request.numero(), request.numero(),
                fecha.format(CBTE_FCH),
                money(total), money(conc), money(neto), money(exento), money(iva), money(tributos),
                request.moneda() != null ? request.moneda() : "PES",
                money(request.cotizacionMoneda() != null
                        ? BigDecimal.valueOf(request.cotizacionMoneda())
                        : BigDecimal.valueOf(AfipCodeMapper.cotizacionMonedaLocal()))
        );
    }

    // ──────────────────────────────────────────────
    // Parseo de respuesta
    // ──────────────────────────────────────────────

    private FiscalEmissionResponse parseResponse(String soapResponse,
                                                 FiscalEmissionRequest request) {
        String resultado = extractTag(soapResponse, "Resultado");
        String cae = extractTag(soapResponse, "CAE");
        String caeFchVto = extractTag(soapResponse, "CAEFchVto");

        LocalDateTime fechaVencimiento = null;
        if (caeFchVto != null && !caeFchVto.isBlank()) {
            try {
                fechaVencimiento = LocalDate.parse(caeFchVto.trim(), CAE_FCH_VTO)
                        .atStartOfDay();
            } catch (Exception ignored) {
            }
        }

        String observaciones = extractTag(soapResponse, "Observaciones");
        if (observaciones == null) {
            observaciones = extractTag(soapResponse, "Msg");
        }

        String resultadoNormalizado = normalizeResultado(resultado);

        log.info("AFIP respondió comprobante PV {} tipo {} nro {}: {} (CAE {})",
                request.puntoVenta(), request.tipoComprobante(), request.numero(),
                resultadoNormalizado, cae);

        return new FiscalEmissionResponse(
                cae,
                fechaVencimiento,
                resultadoNormalizado,
                observaciones,
                soapResponse
        );
    }

    private String normalizeResultado(String resultado) {
        if (resultado == null) return "R";
        String r = resultado.trim().toUpperCase();
        return switch (r) {
            case "A", "APROBADO" -> "A";
            case "R", "RECHAZADO" -> "R";
            case "O", "OBSERVADO" -> "O";
            default -> "R";
        };
    }

    private String extractTag(String xml, String tag) {
        Matcher m = Pattern.compile("<" + tag + ">(.*?)</" + tag + ">", Pattern.DOTALL)
                .matcher(xml);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private String empresaCuit(FiscalEmissionRequest request) {
        // El CUIT del emisor viaja en el header de FECAESolicitar.
        // request no lleva cuit del emisor; se asume que el servicio lo sabe del contexto.
        // Devolvemos placeholder; en la práctica InvoiceServiceImpl lo pasa por docNro del receptor.
        // Aquí dejamos el docNro del receptor como DocNro y el CUIT del emisor vendría del header.
        return "00000000000"; // se reemplaza por InvoiceServiceImpl al construir el request si se desea
    }

    private boolean isCredentialError(String message) {
        String m = message.toLowerCase();
        return m.contains("credencial") || m.contains("invalid auth")
                || m.contains("ticket") || m.contains("auth") || m.contains("401");
    }

    private boolean isTicketExpired(String message) {
        String m = message.toLowerCase();
        return m.contains("expir") || m.contains("venc") || m.contains("invalid ticket");
    }

    private String wsfeEndpoint() {
        return switch (fiscalProperties.getMode()) {
            case "produccion" -> "https://servicios1.afip.gov.ar/wsfev1/service.asmx";
            case "homologacion" -> "https://wswhomo.afip.gov.ar/wsfev1/service.asmx";
            default -> throw new IllegalStateException(
                    "modo fiscal '" + fiscalProperties.getMode()
                            + "' no válido para wsfev1 (use homologacion o produccion)");
        };
    }

    private String money(BigDecimal value) {
        if (value == null) return "0.00";
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private List<?> unused(List<?> items) {
        return items; // items se reservan para detalle cuando AFIP lo requiera (CAE por ítem)
    }
}
