package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Movimiento manual de caja dentro de un turno (retiro o ingreso).
 */
@Entity
@Table(name = "shift_movements", indexes = {
        @Index(name = "idx_movement_shift", columnList = "shift_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shift_id", nullable = false)
    private Long shiftId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ShiftMovementType tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 255)
    private String motivo;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
