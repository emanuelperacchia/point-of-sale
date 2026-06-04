package com.pos.system.event;

/**
 * Evento disparado cuando una venta se completa exitosamente.
 * <p>
 * {@link com.pos.system.service.impl.InvoiceServiceImpl} escucha este evento
 * para emitir el comprobante electrónico correspondiente.
 * </p>
 */
public record SaleCompletedEvent(Long saleId) {
}
