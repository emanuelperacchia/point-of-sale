package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    @Enumerated(EnumType.STRING)
    private PermissionName name;

    @Column(length = 255)
    private String description;

    @ManyToMany(mappedBy = "permissions")
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    public enum PermissionName {
        // Ventas
        VENTAS_READ,
        VENTAS_CREATE,
        VENTAS_UPDATE,
        VENTAS_DELETE,
        VENTAS_REFUND,

        // Compras
        COMPRAS_READ,
        COMPRAS_CREATE,
        COMPRAS_UPDATE,
        COMPRAS_DELETE,

        // Inventario
        INVENTARIO_READ,
        INVENTARIO_CREATE,
        INVENTARIO_UPDATE,
        INVENTARIO_DELETE,
        INVENTARIO_AJUSTE,

        // Usuarios
        USUARIOS_READ,
        USUARIOS_CREATE,
        USUARIOS_UPDATE,
        USUARIOS_DELETE,

        // Reportes
        REPORTES_READ,
        REPORTES_EXPORT,

        // Finanzas
        FINANZAS_READ,
        FINANZAS_CREATE,
        FINANZAS_UPDATE,

        // Admin
        ADMIN_ALL
    }
}
