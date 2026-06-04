package com.pos.system.dto.response;

import com.pos.system.entity.CondicionIva;
import com.pos.system.entity.InvoiceStatus;
import com.pos.system.entity.TipoComprobante;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Respuesta pública con los datos de un comprobante electrónico.
 */
@Builder
public record InvoiceResponse(
        Long id,
        Long saleId,
        TipoComprobante tipoComprobante,
        Integer puntoVenta,
        Long numero,
        String cae,
        LocalDateTime fechaCae,
        InvoiceStatus estado,
        String motivoRechazo,
        String receptorNombre,
        String receptorDocumento,
        CondicionIva receptorCondicionIva,
        LocalDateTime createdAt
) {}
