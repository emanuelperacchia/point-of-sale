package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Devolución de mercadería a proveedor.
 * Gestiona el proceso de devolución por defectos, vencimientos, errores o excesos,
 * y genera notas de crédito automáticas.
 */
@Entity
@Table(name = "supplier_returns")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String returnNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id")
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal unitCost;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false)
    private LocalDate returnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReturnReason reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReturnStatus status = ReturnStatus.PENDING;

    @Column(length = 50)
    private String creditNoteNumber;

    @Column(precision = 10, scale = 2)
    private BigDecimal creditNoteAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(length = 500)
    private String notes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Motivos de devolución.
     */
    public enum ReturnReason {
        DEFECTUOSO("Defectuoso"),
        VENCIDO("Vencido"),
        ERROR_PEDIDO("Error en pedido"),
        EXCESO("Exceso de inventario"),
        OTRO("Otro");

        private final String description;

        ReturnReason(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Estados de la devolución.
     */
    public enum ReturnStatus {
        PENDING("Pendiente"),
        AUTHORIZED("Autorizado"),
        COMPLETED("Completado"),
        REJECTED("Rechazado");

        private final String description;

        ReturnStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
