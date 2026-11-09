package com.pos.system.controller;

import com.pos.system.dto.response.UserResponse;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Gestión de usuarios")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Usuario actual", description = "Obtiene información del usuario autenticado")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(userService.getUserById(userDetails.getId()));
    }

    @GetMapping("/me/permissions")
    @Operation(summary = "Permisos del usuario", description = "Obtiene los permisos del usuario actual")
    public ResponseEntity<Set<String>> getMyPermissions(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(userService.getUserPermissions(userDetails.getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('GERENTE')")
    @Operation(summary = "Obtener usuario por ID", description = "Solo administradores y gerentes")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar usuarios", description = "Solo administradores")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/me/data-export")
    @Operation(summary = "Exportar datos personales", description = "Derecho de acceso/portabilidad (Ley 25.326 art. 14): descarga todos los datos personales que el sistema almacena del usuario autenticado")
    public ResponseEntity<Map<String, Object>> exportMyData(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(userService.exportUserData(userDetails.getId()));
    }

    @PostMapping("/me/delete-request")
    @Operation(summary = "Solicitar supresión de cuenta", description = "Derecho al olvido (Ley 25.326 art. 16): da de baja la cuenta lógicamente, revoca sesiones y preserva registros fiscales por obligación legal")
    public ResponseEntity<Void> requestMyAccountDeletion(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        userService.requestAccountDeletion(userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}
