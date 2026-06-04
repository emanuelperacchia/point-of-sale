package com.pos.system.entity;

/**
 * Condición fiscal del receptor del comprobante.
 * <p>
 * AFIP Argentina: Responsable Inscripto, Monotributista, Exento, Consumidor Final.
 * SII Chile:      similar con matices.
 * </p>
 */
public enum CondicionIva {
    RESPONSABLE_INSCRIPTO,
    MONOTRIBUTISTA,
    EXENTO,
    CONSUMIDOR_FINAL
}
