package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Contacto asociado a un proveedor.
 * Pueden existir múltiples contactos por proveedor (ventas, facturación, logística, etc.).
 */
@Entity
@Table(name = "supplier_contacts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String position;

    @Column(length = 20)
    private String phone;

    @Column(length = 20)
    private String mobile;

    @Column(length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ContactType type = ContactType.GENERAL;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(length = 255)
    private String notes;

    /**
     * Tipo de contacto según su función en la relación comercial.
     */
    public enum ContactType {
        GENERAL,
        SALES,
        BILLING,
        TECHNICAL,
        LOGISTICS
    }
}
