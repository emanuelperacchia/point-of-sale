package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_reconciliations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankReconciliation {

    public enum Estado {
        ABIERTA, CERRADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 7)
    private String periodo; // YYYY-MM format (e.g. 2026-06)

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalExtracto;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalSistema;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal diferencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Estado estado = Estado.ABIERTA;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
