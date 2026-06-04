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
import java.util.ArrayList;
import java.util.List;

/**
 * Proveedor del sistema.
 * Almacena datos fiscales, comerciales y de contacto de cada proveedor.
 */
@Entity
@Table(name = "suppliers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, unique = true, length = 20)
    private String taxId; // RUT/CUIT

    @Column(nullable = false, length = 200)
    private String businessName;

    @Column(length = 200)
    private String tradeName;

    @Column(length = 255)
    private String address;

    @Column(length = 50)
    private String city;

    @Column(length = 50)
    private String state;

    @Column(length = 20)
    private String postalCode;

    @Column(length = 50)
    private String country;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 100)
    private String website;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private SupplierCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentTerm paymentTerm = PaymentTerm.NET_30;

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal currentDebt = BigDecimal.ZERO;

    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(length = 500)
    private String notes;

    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SupplierContact> contacts = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Plazos de pago disponibles para proveedores.
     */
    public enum PaymentTerm {
        IMMEDIATE("Inmediato", 0),
        NET_7("7 días", 7),
        NET_15("15 días", 15),
        NET_30("30 días", 30),
        NET_45("45 días", 45),
        NET_60("60 días", 60),
        NET_90("90 días", 90);

        private final String description;
        private final int days;

        PaymentTerm(String description, int days) {
            this.description = description;
            this.days = days;
        }

        public String getDescription() {
            return description;
        }

        public int getDays() {
            return days;
        }
    }

    // -- Helper methods --

    public void addContact(SupplierContact contact) {
        contacts.add(contact);
        contact.setSupplier(this);
    }

    public void removeContact(SupplierContact contact) {
        contacts.remove(contact);
        contact.setSupplier(null);
    }

    /**
     * Verifica si el proveedor tiene crédito disponible para una operación.
     *
     * @param amount monto a verificar
     * @return true si tiene crédito disponible o no tiene límite
     */
    public boolean hasAvailableCredit(BigDecimal amount) {
        if (creditLimit.compareTo(BigDecimal.ZERO) == 0) {
            return true; // Sin límite
        }
        return currentDebt.add(amount).compareTo(creditLimit) <= 0;
    }
}
