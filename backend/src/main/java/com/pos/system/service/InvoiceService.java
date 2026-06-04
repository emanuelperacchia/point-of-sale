package com.pos.system.service;

import com.pos.system.dto.response.InvoiceResponse;
import com.pos.system.entity.InvoiceStatus;
import com.pos.system.entity.TipoComprobante;
import org.springframework.core.io.Resource;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de emisión de comprobantes electrónicos.
 * <p>
 * Orquesta el pipeline: generación de XML → firma digital → envío al organismo fiscal.
 * </p>
 */
public interface InvoiceService {

    /**
     * Emite un comprobante electrónico para una venta.
     * <p>
     * Si ya existe un comprobante para esa venta, retorna el existente (idempotente).
     * </p>
     *
     * @param saleId ID de la venta
     * @return comprobante emitido
     */
    InvoiceResponse emitInvoice(Long saleId);

    /**
     * Reintenta la emisión de un comprobante en estado PENDIENTE.
     *
     * @param invoiceId ID del comprobante
     * @return comprobante con estado actualizado
     */
    InvoiceResponse retryEmission(Long invoiceId);

    /**
     * Obtiene un comprobante por su ID.
     */
    InvoiceResponse getById(Long id);

    /**
     * Busca comprobantes con filtros opcionales.
     */
    List<InvoiceResponse> findByFilters(
            TipoComprobante tipo,
            InvoiceStatus estado,
            LocalDateTime desde,
            LocalDateTime hasta,
            Long saleId
    );

    /**
     * Obtiene el PDF del comprobante como recurso descargable.
     *
     * @param invoiceId ID del comprobante
     * @return resource del archivo PDF
     */
    Resource getPdf(Long invoiceId);
}
