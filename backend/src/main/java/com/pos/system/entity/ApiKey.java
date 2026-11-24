package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AfipCodeMapper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(length = 20)
    private String taxId;

    @Column(length = 100)
    private String businessName;

    @Column(length = 100)
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
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 100)
    private String website;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean termsAccepted = false;

    @Column(length = 50)
    private String rateLimit;

    @Column(length = 50)
    private String expirationInterval;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum PermissionName {
        ADMIN_ALL,
        VENTAS_READ, VENTAS_CREATE, VENTAS_UPDATE, VENTAS_DELETE, VENTAS_REFUND,
        COMPRAS_READ, COMPRAS_CREATE, COMPRAS_UPDATE, COMPRAS_DELETE,
        INVENTARIO_READ, INVENTARIO_CREATE, INVENTARIO_UPDATE, INVENTARIO_DELETE, INVENTARIO_AJUSTE,
        USUARIOS_READ, USUARIOS_CREATE, USUARIOS_UPDATE, USUARIOS_DELETE,
        REPORTES_READ, REPORTES_EXPORT,
        FINANZAS_READ, FINANZAS_CREATE, FINANZAS_UPDATE
    }
}
