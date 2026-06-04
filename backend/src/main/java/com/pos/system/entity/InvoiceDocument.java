package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Documento de comprobante electrónico (boleta, factura, NC, ND).
 * <p>
 * Se crea al procesar una venta (Sale) y pasa por los estados
 * PENDIENTE → EMITIDO / RECHAZADO.
 * </p>
 */
@Entity
@Table(name = "invoice_documents", indexes = {
        @Index(name = "idx_invoice_sale", columnList = "sale_id", unique = true),
        @Index(name = "idx_invoice_estado", columnList = "estado"),
        @Index(name = "idx_invoice_tipo_numero", columnList = "tipo_comprobante,numero")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_id", nullable = false)
    private Long saleId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TipoComprobante tipoComprobante;

    @Column(nullable = false)
    @Builder.Default
    private Integer puntoVenta = 1;

    @Column(nullable = false)
    private Long numero;

    @Column(length = 20)
    private String cae;

    private LocalDateTime fechaCae;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String xmlFirmado;

    @Column(length = 500)
    private String pdfPath;

    @Column(columnDefinition = "TEXT")
    private String qrCode;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private InvoiceStatus estado;

    @Column(nullable = false)
    @Builder.Default
    private Integer intentos = 0;

    @Column(columnDefinition = "TEXT")
    private String motivoRechazo;

    /** Para Nota de Crédito / Débito: comprobante original */
    private Long comprobanteOriginalId;

    // --- Datos del receptor (desnormalizados para consistencia histórica) ---

    @Column(length = 200)
    private String receptorNombre;

    @Column(length = 50)
    private String receptorDocumento;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private CondicionIva receptorCondicionIva;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
