package com.pos.system.service;

import com.pos.system.dto.request.CreateReturnRequest;
import com.pos.system.dto.response.SaleReturnResponse;

import java.util.List;

/**
 * Servicio de devoluciones de ventas.
 */
public interface SaleReturnService {

    /**
     * Crea una devolución. Si el monto es menor o igual al límite configurado,
     * se auto-aprueba; caso contrario queda PENDIENTE_APROBACION.
     */
    SaleReturnResponse createReturn(CreateReturnRequest request, Long usuarioId);

    /**
     * Aprueba una devolución pendiente: reintegra stock y actualiza estado.
     */
    SaleReturnResponse approveReturn(Long returnId, Long aprobadorId);

    /**
     * Rechaza una devolución pendiente.
     */
    SaleReturnResponse rejectReturn(Long returnId, Long aprobadorId);

    /** Obtiene una devolución por ID. */
    SaleReturnResponse getById(Long id);

    /** Lista devoluciones de una venta. */
    List<SaleReturnResponse> findBySaleId(Long saleId);
}
