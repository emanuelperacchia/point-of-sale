package com.pos.system.entity.system;

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
        ADMIN_ALL,
        VENTAS_READ, VENTAS_CREATE, VENTAS_UPDATE, VENTAS_DELETE, VENTAS_REFUND,
        COMPRAS_READ, COMPRAS_CREATE, COMPRAS_UPDATE, COMPRAS_DELETE,
        INVENTARIO_READ, INVENTARIO_CREATE, INVENTARIO_UPDATE, INVENTARIO_DELETE, INVENTARIO_AJUSTE,
        USUARIOS_READ, USUARIOS_CREATE, USUARIOS_UPDATE, USUARIOS_DELETE,
        REPORTES_READ, REPORTES_EXPORT,
        FINANZAS_READ, FINANZAS_CREATE, FINANZAS_UPDATE
    }
}
