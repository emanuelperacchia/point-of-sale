package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Turno de caja: apertura, cierre y control de efectivo.
 * <p>
 * Un cajero solo puede tener un turno ABIERTO a la vez.
 * Todas las ventas en efectivo se vinculan al turno activo.
 * </p>
 */
@Entity
@Table(name = "cash_shifts", indexes = {
        @Index(name = "idx_shift_cajero_estado", columnList = "cajero_id, estado")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cajero_id", nullable = false)
    private User cajero;

    @Column(nullable = false)
    @Builder.Default
    private Long sucursalId = 1L;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ShiftStatus estado;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montoApertura;

    @Column(precision = 12, scale = 2)
    private BigDecimal montoCierre;

    @Column(precision = 12, scale = 2)
    private BigDecimal diferencia;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaApertura;

    private LocalDateTime fechaCierre;
}
