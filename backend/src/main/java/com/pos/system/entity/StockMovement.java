package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovementType type;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(precision = 10, scale = 2)
    private BigDecimal previousStock;

    @Column(precision = 10, scale = 2)
    private BigDecimal currentStock;

    @Column(precision = 10, scale = 2)
    private BigDecimal unitCost;

    @Column(length = 500)
    private String reason;

    @Column(length = 100)
    private String referenceDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum MovementType {
        ENTRADA_COMPRA,        // Entrada por compra
        ENTRADA_DEVOLUCION,    // Entrada por devolución cliente
        ENTRADA_AJUSTE,        // Ajuste positivo
        ENTRADA_TRANSFERENCIA, // Transferencia desde otra bodega
        SALIDA_VENTA,          // Salida por venta
        SALIDA_DEVOLUCION,     // Devolución a proveedor
        SALIDA_AJUSTE,         // Ajuste negativo
        SALIDA_TRANSFERENCIA,  // Transferencia a otra bodega
        SALIDA_MERMA           // Pérdida/merma
    }
}