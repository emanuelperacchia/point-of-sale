package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ExpenseCategory categoria;

    @Column(name = "proveedor_id")
    private Long proveedorId;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ExpenseEstado estado;

    @Column(name = "comprobante_url", length = 500)
    private String comprobanteUrl;

    @Column(nullable = false)
    private Boolean recurrente;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private ExpenseFrecuencia frecuencia;

    @Column(name = "proxima_fecha")
    private LocalDate proximaFecha;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Enums ──────────────────────────────────────────────────────────

    public enum ExpenseCategory {
        ALQUILER, SERVICIOS, SUELDOS, COMPRAS_MERCADERIA,
        IMPUESTOS, MANTENIMIENTO, MARKETING, OTROS
    }

    public enum ExpenseEstado {
        PAGADO, PENDIENTE
    }

    public enum ExpenseFrecuencia {
        MENSUAL, TRIMESTRAL, ANUAL
    }
}
