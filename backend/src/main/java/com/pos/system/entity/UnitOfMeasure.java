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
@Table(name = "units_of_measure")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnitOfMeasure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 10)
    private String symbol;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UnitCategory category;

    @Column(nullable = false)
    @Builder.Default
    private Boolean baseUnit = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_unit_id")
    private UnitOfMeasure baseUnitRef;

    @Column(precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal conversionFactor = BigDecimal.ONE;

    @Column(nullable = false)
    @Builder.Default
    private Integer decimalPlaces = 2;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum UnitCategory {
        WEIGHT,     // Peso: kg, g, lb, oz
        VOLUME,     // Volumen: L, ml, gal
        LENGTH,     // Longitud: m, cm, in
        AREA,       // Área: m2, cm2
        UNIT,       // Unidad: pieza, caja, pack, docena
        TIME,       // Tiempo: hora, día, mes
        OTHER       // Otros
    }
}