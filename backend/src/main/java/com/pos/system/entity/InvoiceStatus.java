package com.pos.system.entity;

/**
 * Estado del comprobante electrónico a lo largo de su ciclo de vida.
 * <p>
 * PENDIENTE → EMITIDO  (happy path: se obtuvo CAE/CAF del organismo fiscal)
 * PENDIENTE → RECHAZADO (el organismo rechazó el comprobante)
 * EMITIDO   → ANULADO   (se anula administrativamente)
 * </p>
 */
public enum InvoiceStatus {
    PENDIENTE,
    EMITIDO,
    RECHAZADO,
    ANULADO
}
