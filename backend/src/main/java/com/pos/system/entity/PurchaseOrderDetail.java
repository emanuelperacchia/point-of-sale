package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Detalle de una orden de compra.
 * Cada línea representa un producto solicitado con su cantidad, precio y descuento.
 */
@Entity
@Table(name = "purchase_order_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal quantityReceived = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal unitPrice;

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(length = 255)
    private String notes;

    // -- Transient (calculated) fields --

    @Transient
    public BigDecimal getPendingQuantity() {
        return quantity.subtract(quantityReceived);
    }

    @Transient
    public boolean isFullyReceived() {
        return quantityReceived.compareTo(quantity) >= 0;
    }

    @Transient
    public BigDecimal getDiscountAmount() {
        return unitPrice.multiply(quantity)
                .multiply(discountPercentage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    // -- Lifecycle callbacks --

    /**
     * Calcula el subtotal antes de persistir o actualizar.
     */
    @PrePersist
    @PreUpdate
    public void calculateSubtotal() {
        BigDecimal lineTotal = unitPrice.multiply(quantity);
        BigDecimal discount = getDiscountAmount();
        this.subtotal = lineTotal.subtract(discount).add(taxAmount);
    }
}
