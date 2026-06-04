package com.pos.system.service;

import java.time.LocalDateTime;

/**
 * Abstraction over the fiscal authority API (AFIP/SII/…).
 * <p>
 * Implementaciones: {@link com.pos.system.service.impl.MockFiscalApiClient} (mock),
 * y futuras implementaciones reales para homologación y producción.
 * </p>
 */
public interface FiscalApiClient {

    /**
     * Envía un comprobante fiscalmente firmado al organismo de recaudación.
     *
     * @param xmlFirmado    XML del comprobante firmado digitalmente
     * @param tipoComprobante tipo de comprobante (FACTURA_A, BOLETA, etc.)
     * @param numero        número correlativo del comprobante
     * @param puntoVenta    punto de venta registrado
     * @return respuesta del organismo fiscal
     */
    FiscalEmissionResponse emitirComprobante(
            String xmlFirmado,
            String tipoComprobante,
            Long numero,
            Integer puntoVenta
    );

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
}
