package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Categoría de proveedor para clasificar las relaciones comerciales.
 */
@Entity
@Table(name = "supplier_categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
