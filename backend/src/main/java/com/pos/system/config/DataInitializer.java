package com.pos.system.config;

import com.pos.system.entity.permission.Permission;
import com.pos.system.entity.system.Role;
import com.pos.system.entity.system.User;
import com.pos.system.repository.permission.PermissionRepository;
import com.pos.system.repository.system.RoleRepository;
import com.pos.system.repository.system.UserRepository;
import com.pos.system.security.PasswordEncoder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                           PermissionRepository permissionRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (roleRepository.count() > 0) {
            return; // Ya existe inicialización
        }

        // Crear permisos
        Permission adminAll = createPermission("ADMIN_ALL", "Acceso total");
        Permission ventasRead = createPermission("VENTAS_READ", "Ver ventas");
        Permission ventasCreate = createPermission("VENTAS_CREATE", "Crear ventas");
        Permission ventasUpdate = createPermission("VENTAS_UPDATE", "Actualizar ventas");
        Permission ventasDelete = createPermission("VENTAS_DELETE", "Eliminar ventas");
        Permission ventasRefund = createPermission("VENTAS_REFUND", "Reembolsar ventas");

        Permission comprasRead = createPermission("COMPRAS_READ", "Ver compras");
        Permission comprasCreate = createPermission("COMPRAS_CREATE", "Crear compras");
        Permission comprasUpdate = createPermission("COMPRAS_UPDATE", "Actualizar compras");
        Permission comprasDelete = createPermission("COMPRAS_DELETE", "Eliminar compras");

        Permission inventarioRead = createPermission("INVENTARIO_READ", "Ver inventario");
        Permission inventarioCreate = createPermission("INVENTARIO_CREATE", "Crear inventario");
        Permission inventarioUpdate = createPermission("INVENTARIO_UPDATE", "Actualizar inventario");
        Permission inventarioDelete = createPermission("INVENTARIO_DELETE", "Eliminar inventario");
        Permission inventarioAjuste = createPermission("INVENTARIO_AJUSTE", "Ajustar inventario");

        Permission usuariosRead = createPermission("USUARIOS_READ", "Ver usuarios");
        Permission usuariosCreate = createPermission("USUARIOS_CREATE", "Crear usuarios");
        Permission usuariosUpdate = createPermission("USUARIOS_UPDATE", "Actualizar usuarios");
        Permission usuariosDelete = createPermission("USUARIOS_DELETE", "Eliminar usuarios");

        Permission reportesRead = createPermission("REPORTES_READ", "Ver reportes");
        Permission reportesExport = createPermission("REPORTES_EXPORT", "Exportar reportes");

        Permission finanzasRead = createPermission("FINANZAS_READ", "Ver finanzas");
        Permission finanzasCreate = createPermission("FINANZAS_CREATE", "Crear finanzas");
        Permission finanzasUpdate = createPermission("FINANZAS_UPDATE", "Actualizar finanzas");

        // Crear roles
        Role adminRole = Role.builder()
                .name(Role.RoleName.ADMIN)
                .description("Administrador del sistema")
                .permissions(java.util.Set.of(
                        adminAll, ventasRead, ventasCreate, ventasUpdate, ventasDelete, ventasRefund,
                        comprasRead, comprasCreate, comprasUpdate, comprasDelete,
                        inventarioRead, inventarioCreate, inventarioUpdate, inventarioDelete, inventarioAjuste,
                        usuariosRead, usuariosCreate, usuariosUpdate, usuariosDelete,
                        reportesRead, reportesExport,
                        finanzasRead, finanzasCreate, finanzasUpdate
                ))
                .build();

        Role gerenteRole = Role.builder()
                .name(Role.RoleName.GERENTE)
                .description("Gerente de tienda")
                .permissions(java.util.Set.of(
                        ventasRead, ventasCreate, ventasUpdate, ventasDelete, ventasRefund,
                        comprasRead, comprasCreate, comprasUpdate, comprasDelete,
                        inventarioRead, inventarioCreate, inventarioUpdate, inventarioDelete, inventarioAjuste,
                        usuariosRead,
                        reportesRead, reportesExport,
                        finanzasRead, finanzasCreate, finanzasUpdate
                ))
                .build();

        Role cajeroRole = Role.builder()
                .name(Role.RoleName.CAJERO)
                .description("Cajero")
                .permissions(java.util.Set.of(ventasRead, ventasCreate))
                .build();

        Role vendedorRole = Role.builder()
                .name(Role.RoleName.VENDEDOR)
                .description("Vendedor")
                .permissions(java.util.Set.of(ventasRead))
                .build();

        Role inventarioRole = Role.builder()
                .name(Role.RoleName.INVENTARIO)
                .description("Encargado de inventario")
                .permissions(java.util.Set.of(
                        comprasRead,
                        inventarioRead, inventarioCreate, inventarioUpdate, inventarioDelete, inventarioAjuste
                ))
                .build();

        roleRepository.save(adminRole);
        roleRepository.save(gerenteRole);
        roleRepository.save(cajeroRole);
        roleRepository.save(vendedorRole);
        roleRepository.save(inventarioRole);

        // Crear usuario admin
        User admin = User.builder()
                .email("admin@pos.com")
                .password(passwordEncoder.encode("admin123"))
                .firstName("Admin")
                .lastName("System")
                .active(true)
                .emailVerified(true)
                .termsAccepted(true)
                .roles(java.util.Set.of(adminRole))
                .build();
        userRepository.save(admin);

        System.out.println("================================================");
        System.out.println("Sistema Nivex POS inicializado correctamente");
        System.out.println("Usuario: admin@pos.com");
        System.out.println("Contraseña: admin123");
        System.out.println("================================================");
    }

    private Permission createPermission(String name, String description) {
        return permissionRepository.save(Permission.builder()
                .name(com.pos.system.entity.system.Permission.PermissionName.valueOf(name))
                .description(description)
                .build());
    }
}
