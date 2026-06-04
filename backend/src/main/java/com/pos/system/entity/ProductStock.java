package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_stocks", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "warehouse_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal currentStock = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal reservedStock = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minimumStock = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal maximumStock = BigDecimal.ZERO;

    @Column(precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal averageCost = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalValue = BigDecimal.ZERO;

    @UpdateTimestamp
    private LocalDateTime lastMovement;

    // Helper methods
    public BigDecimal getAvailableStock() {
        return currentStock.subtract(reservedStock);
    }

    public boolean isBelowMinimum() {
        return currentStock.compareTo(minimumStock) < 0;
    }

    public boolean isAboveMaximum() {
        return currentStock.compareTo(maximumStock) > 0;
    }

    public BigDecimal getSuggestedPurchaseQuantity() {
        if (currentStock.compareTo(minimumStock) >= 0) {
            return BigDecimal.ZERO;
        }
        return maximumStock.subtract(currentStock);
    }
}