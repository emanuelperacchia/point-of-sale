package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Devolución de una venta (parcial o total).
 * <p>
 * Al aprobarse, se reintegra el stock y se genera la nota de crédito
 * (la NC fiscal completa va en Sprint 8).
 * </p>
 */
@Entity
@Table(name = "sale_returns")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_id", nullable = false)
    private Long saleId;

    @Column(nullable = false, length = 25)
    @Enumerated(EnumType.STRING)
    private ReturnStatus estado;

    @Column(nullable = false, length = 500)
    private String motivo;

    /** ID del usuario que aprobó (nullable si auto-aprobada o pendiente) */
    private Long aprobadorId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentMethod metodoDevolucion;

    /** Invoice ID de la nota de crédito generada (null hasta Sprint 8 NC fiscal) */
    private Long notaCreditoId;

    @Column(length = 50)
    private String referenciaDevolucion;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
