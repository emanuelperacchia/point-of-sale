package com.pos.system.service;

import com.pos.system.entity.CondicionIva;
import com.pos.system.entity.TipoComprobante;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Abstraction over the fiscal authority API (AFIP/ARCA wsfev1, SII, …).
 * <p>
 * El contrato es ahora <b>estructurado</b>: se envían los campos del
 * comprobante (no XML firmado), porque AFIP wsfev1 {@code FECAESolicitar}
 * recibe campos sueltos tras autenticarse con WSAA (Ticket de Acceso).
 * </p>
 *
 * Implementaciones:
 * <ul>
 *   <li>{@link com.pos.system.service.impl.MockFiscalApiClient} (mock, por defecto)</li>
 *   <li>{@link com.pos.system.service.afip.AfipFiscalApiClient} (AFIP real, homologación/producción)</li>
 * </ul>
 */
public interface FiscalApiClient {

    /**
     * Datos estructurados del comprobante a emitir ante el organismo fiscal.
     */
    record FiscalEmissionRequest(
            Integer puntoVenta,
            TipoComprobante tipoComprobante,
            Long numero,
            LocalDateTime fechaEmision,
            // Concepto AFIP: 1 = productos, 2 = servicios, 3 = ambos
            Integer concepto,
            Long docTipo,            // código AFIP de tipo de documento del receptor
            String docNro,           // número de documento del receptor
            String razonSocial,      // razón social / nombre del receptor
            String domicilio,        // domicilio fiscal del receptor
            CondicionIva condicionIvaReceptor,
            BigDecimal importeNeto,
            BigDecimal importeIva,
            BigDecimal importeTotal,
            BigDecimal importeExento,
            BigDecimal importeTributos,
            String moneda,           // "PES"
            Double cotizacionMoneda,
            List<AfipItem> items
    ) {}

    /**
     * Ítem de detalle del comprobante para AFIP.
     */
    record AfipItem(
            String codigo,
            String descripcion,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal importeBonificacion,
            BigDecimal importeIva,
            BigDecimal importeTotal
    ) {}

    /**
     * Respuesta del organismo fiscal ante una solicitud de emisión.
     *
     * @param cae             Código de Autorización Electrónico (null si fue rechazado)
     * @param fechaVencimiento fecha de vencimiento del CAE
     * @param resultado       "A" = Aprobado, "R" = Rechazado, "O" = Observado
     * @param observaciones   detalle del resultado (motivo de rechazo, advertencias)
     * @param xmlResponse     XML de respuesta completo del organismo (para trazabilidad)
     */
    record FiscalEmissionResponse(
            String cae,
            LocalDateTime fechaVencimiento,
            String resultado,
            String observaciones,
            String xmlResponse
    ) {}

    /**
     * Envía el comprobante al organismo fiscal y devuelve la autorización (CAE).
     *
     * @param request datos estructurados del comprobante
     * @return respuesta del organismo fiscal
     */
    FiscalEmissionResponse emitirComprobante(FiscalEmissionRequest request);
}
