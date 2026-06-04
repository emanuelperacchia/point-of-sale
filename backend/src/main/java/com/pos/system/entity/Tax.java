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
@Table(name = "taxes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tax {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal rate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaxType type = TaxType.INCLUDED;

    @Column(length = 50)
    private String region;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum TaxType {
        INCLUDED,   // IVA incluido en el precio
        ADDED,      // Se agrega al precio
        EXEMPT      // Exento de impuesto
    }

    /**
     * Calcula el monto del impuesto sobre un precio base
     */
    public BigDecimal calculateTax(BigDecimal basePrice) {
        if (type == TaxType.EXEMPT) {
            return BigDecimal.ZERO;
        }
        return basePrice.multiply(rate).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Calcula el precio final incluyendo este impuesto
     */
    public BigDecimal calculateFinalPrice(BigDecimal basePrice) {
        if (type == TaxType.INCLUDED) {
            return basePrice;
        }
        if (type == TaxType.EXEMPT) {
            return basePrice;
        }
        // ADDED: precio base + impuesto
        return basePrice.add(calculateTax(basePrice));
    }
}