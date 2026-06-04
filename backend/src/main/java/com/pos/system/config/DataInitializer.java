package com.pos.system.config;

import com.pos.system.entity.Permission;
import com.pos.system.entity.Permission.PermissionName;
import com.pos.system.entity.Role;
import com.pos.system.entity.Role.RoleName;
import com.pos.system.entity.User;
import com.pos.system.repository.PermissionRepository;
import com.pos.system.repository.RoleRepository;
import com.pos.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (roleRepository.count() > 0) {
            log.info("Los datos iniciales ya existen, omitiendo inicialización");
            return;
        }

        log.info("Inicializando roles y permisos...");

        Permission adminPerm = createPermission(PermissionName.ADMIN_ALL, "Acceso total");

        Permission ventasPerm = createPermission(PermissionName.VENTAS_READ, "Ver ventas");
        Permission ventasCreate = createPermission(PermissionName.VENTAS_CREATE, "Crear ventas");
        Permission ventasUpdate = createPermission(PermissionName.VENTAS_UPDATE, "Actualizar ventas");
        Permission ventasDelete = createPermission(PermissionName.VENTAS_DELETE, "Eliminar ventas");
        Permission ventasRefund = createPermission(PermissionName.VENTAS_REFUND, "Reembolsar ventas");

        Permission comprasRead = createPermission(PermissionName.COMPRAS_READ, "Ver compras");
        Permission comprasCreate = createPermission(PermissionName.COMPRAS_CREATE, "Crear compras");
        Permission comprasUpdate = createPermission(PermissionName.COMPRAS_UPDATE, "Actualizar compras");
        Permission comprasDelete = createPermission(PermissionName.COMPRAS_DELETE, "Eliminar compras");

        Permission inventarioPerm = createPermission(PermissionName.INVENTARIO_READ, "Ver inventario");
        Permission inventarioCreate = createPermission(PermissionName.INVENTARIO_CREATE, "Crear inventario");
        Permission inventarioUpdate = createPermission(PermissionName.INVENTARIO_UPDATE, "Actualizar inventario");
        Permission inventarioDelete = createPermission(PermissionName.INVENTARIO_DELETE, "Eliminar inventario");
        Permission inventarioAjuste = createPermission(PermissionName.INVENTARIO_AJUSTE, "Ajustar inventario");

        Permission usuariosRead = createPermission(PermissionName.USUARIOS_READ, "Ver usuarios");
        Permission usuariosCreate = createPermission(PermissionName.USUARIOS_CREATE, "Crear usuarios");
        Permission usuariosUpdate = createPermission(PermissionName.USUARIOS_UPDATE, "Actualizar usuarios");
        Permission usuariosDelete = createPermission(PermissionName.USUARIOS_DELETE, "Eliminar usuarios");

        Permission reportesRead = createPermission(PermissionName.REPORTES_READ, "Ver reportes");
        Permission reportesExport = createPermission(PermissionName.REPORTES_EXPORT, "Exportar reportes");

        Permission finanzasRead = createPermission(PermissionName.FINANZAS_READ, "Ver finanzas");
        Permission finanzasCreate = createPermission(PermissionName.FINANZAS_CREATE, "Crear finanzas");
        Permission finanzasUpdate = createPermission(PermissionName.FINANZAS_UPDATE, "Actualizar finanzas");

        Role adminRole = Role.builder()
                .name(RoleName.ADMIN)
                .description("Administrador del sistema")
                .permissions(Set.of(adminPerm))
                .build();

        Role gerenteRole = Role.builder()
                .name(RoleName.GERENTE)
                .description("Gerente de tienda")
                .permissions(Set.of(
                        ventasPerm, ventasCreate, ventasUpdate, ventasDelete, ventasRefund,
                        comprasRead, comprasCreate, comprasUpdate, comprasDelete,
                        inventarioPerm, inventarioCreate, inventarioUpdate, inventarioDelete, inventarioAjuste,
                        usuariosRead,
                        reportesRead, reportesExport,
                        finanzasRead, finanzasCreate, finanzasUpdate
                ))
                .build();

        Role cajeroRole = Role.builder()
                .name(RoleName.CAJERO)
                .description("Cajero")
                .permissions(Set.of(ventasPerm, ventasCreate))
                .build();

        Role vendedorRole = Role.builder()
                .name(RoleName.VENDEDOR)
                .description("Vendedor")
                .permissions(Set.of(ventasPerm))
                .build();

        Role inventarioRole = Role.builder()
                .name(RoleName.INVENTARIO)
                .description("Encargado de inventario")
                .permissions(Set.of(
                        comprasRead,
                        inventarioPerm, inventarioCreate, inventarioUpdate, inventarioDelete, inventarioAjuste
                ))
                .build();

        roleRepository.save(adminRole);
        roleRepository.save(gerenteRole);
        roleRepository.save(cajeroRole);
        roleRepository.save(vendedorRole);
        roleRepository.save(inventarioRole);

        User admin = User.builder()
                .email("admin@pos.com")
                .password(passwordEncoder.encode("admin123"))
                .firstName("Admin")
                .lastName("System")
                .active(true)
                .emailVerified(true)
                .build();
        admin.getRoles().add(adminRole);
        userRepository.save(admin);

        log.info("Datos inicializados correctamente. Usuario admin creado: {}", admin.getEmail());
    }

    private Permission createPermission(PermissionName name, String description) {
        return permissionRepository.save(Permission.builder()
                .name(name)
                .description(description)
                .build());
    }
}
