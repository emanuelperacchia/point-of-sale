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
import java.util.ArrayList;
import java.util.List;

/**
 * Orden de compra a proveedor.
 * Gestiona el ciclo de vida completo: borrador, enviada, confirmada, recibida o cancelada.
 */
@Entity
@Table(name = "purchase_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(nullable = false)
    private LocalDate orderDate;

    private LocalDate expectedDeliveryDate;

    private LocalDate deliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.BORRADOR;

    @Column(precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Supplier.PaymentTerm paymentTerm;

    @Column(length = 500)
    private String notes;

    @Column(length = 100)
    private String contactPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private LocalDateTime approvedAt;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PurchaseOrderDetail> details = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Estados posibles de una orden de compra.
     */
    public enum OrderStatus {
        BORRADOR("Borrador"),
        ENVIADA("Enviada"),
        CONFIRMADA("Confirmada"),
        RECIBIDA_PARCIAL("Recibida Parcial"),
        RECIBIDA_COMPLETA("Recibida Completa"),
        CANCELADA("Cancelada");

        private final String description;

        OrderStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // -- Helper methods --

    public void addDetail(PurchaseOrderDetail detail) {
        details.add(detail);
        detail.setPurchaseOrder(this);
        recalculateTotals();
    }

    public void removeDetail(PurchaseOrderDetail detail) {
        details.remove(detail);
        detail.setPurchaseOrder(null);
        recalculateTotals();
    }

    public void recalculateTotals() {
        this.subtotal = details.stream()
                .map(PurchaseOrderDetail::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discountedSubtotal = subtotal.subtract(discountAmount);
        this.total = discountedSubtotal.add(taxAmount);
    }

    public boolean canBeModified() {
        return status == OrderStatus.BORRADOR;
    }

    public boolean canBeCancelled() {
        return status != OrderStatus.RECIBIDA_COMPLETA
                && status != OrderStatus.CANCELADA;
    }

    public BigDecimal getTotalQuantityOrdered() {
        return details.stream()
                .map(PurchaseOrderDetail::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalQuantityReceived() {
        return details.stream()
                .map(PurchaseOrderDetail::getQuantityReceived)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isFullyReceived() {
        return status == OrderStatus.RECIBIDA_COMPLETA;
    }

    public boolean isOverdue() {
        if (expectedDeliveryDate == null || isFullyReceived()) {
            return false;
        }
        return LocalDate.now().isAfter(expectedDeliveryDate);
    }
}
