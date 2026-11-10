package com.pos.system.service.afip;

import com.pos.system.entity.CondicionIva;
import com.pos.system.entity.TipoComprobante;

/**
 * Mapeo de códigos internos de Nivex a códigos de AFIP (wsfev1).
 * <p>
 * Referencia: códigos de tipo de comprobante (CbteTipo) y tipo de documento
 * (DocTipo) del servicio de Factura Electrónica de AFIP.
 * </p>
 */
public final class AfipCodeMapper {

    private AfipCodeMapper() {
    }

    /**
     * Códigos CbteTipo de AFIP (Factura Electrónica).
     */
    public static int cbteTipo(com.pos.system.entity.TipoComprobante tipo) {
        return switch (tipo) {
            case FACTURA_A -> 1;
            case FACTURA_B -> 6;
            case FACTURA_C -> 11;
            case BOLETA -> 11; // boleta al consumidor final → Factura C (cbteTipo 11)
            case NOTA_CREDITO -> 3;
            case NOTA_DEBITO -> 2;
        };
    }

    /**
     * Códigos DocTipo de AFIP para el documento del receptor.
     */
    public static int docTipo(String documentType) {
        return switch (documentType != null ? documentType.toUpperCase() : "") {
            case "CUIT" -> 80;
            case "CUIL" -> 86;
            case "DNI" -> 96;
            case "LC" -> 94;
            case "LE" -> 90;
            case "PASAPORTE" -> 96;
            default -> 99; // otro / no especificado
        };
    }

    /**
     * Concepto AFIP para FECAESolicitar: 1 = productos, 2 = servicios, 3 = ambos.
     * Nivex vende productos → 1.
     */
    public static int conceptoProductos() {
        return 1;
    }

    /**
     * Indica si el tipo de comprobante requiere receptor Responsable Inscripto
     * (regla de AFIP: Factura A solo a Responsable Inscripto).
     */
    public static boolean requiresResponsableInscrito(com.pos.system.entity.TipoComprobante tipo) {
        return tipo == com.pos.system.entity.TipoComprobante.FACTURA_A;
    }

    public static boolean esMonedaPesos(String moneda) {
        return "PES".equalsIgnoreCase(moneda);
    }

    public static double cotizacionMonedaLocal() {
        return 1.0;
    }

    public static int condicionIvaReceptor(CondicionIva condicion) {
        return switch (condicion) {
            case RESPONSABLE_INSCRIPTO -> 1;
            case MONOTRIBUTISTA -> 3;
            case EXENTO -> 4;
            case CONSUMIDOR_FINAL -> 5;
        };
    }
}
