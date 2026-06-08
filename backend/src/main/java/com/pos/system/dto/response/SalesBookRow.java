package com.pos.system.dto.response;

import com.pos.system.entity.InvoiceStatus;
import com.pos.system.entity.TipoComprobante;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Una fila del libro de ventas.
 */
public record SalesBookRow(
        LocalDateTime fecha,
        TipoComprobante tipoComprobante,
        Integer puntoVenta,
        Long numero,
        String cuitReceptor,
        String razonSocial,
        BigDecimal netoGravado,
        BigDecimal iva,
        BigDecimal otrosImpuestos,
        BigDecimal total,
        InvoiceStatus estado
) {
    public String getNumeroFormateado() {
        return String.format("%04d-%08d", puntoVenta, numero);
    }
}
