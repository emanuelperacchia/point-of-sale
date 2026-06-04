package com.pos.system.controller;

import com.pos.system.dto.request.LoginRequest;
import com.pos.system.dto.request.RefreshTokenRequest;
import com.pos.system.dto.request.RegisterRequest;
import com.pos.system.dto.response.AuthResponse;
import com.pos.system.dto.response.ErrorResponse;
import com.pos.system.dto.response.UserResponse;
import com.pos.system.exception.AuthenticationException;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints para autenticación JWT")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica usuario y retorna tokens JWT")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIp(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            AuthResponse response = authService.login(request, ipAddress, userAgent);
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            ErrorResponse error = new ErrorResponse(
                    HttpStatus.UNAUTHORIZED.value(),
                    "Unauthorized",
                    e.getMessage(),
                    "/api/auth/login",
                    LocalDateTime.now()
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar usuario", description = "Crea un nuevo usuario en el sistema")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refrescar token", description = "Renueva el access token usando refresh token")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(authService.refreshToken(request, userId));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión", description = "Invalida el refresh token")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshTokenRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            HttpServletRequest httpRequest) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        String ipAddress = getClientIp(httpRequest);
        authService.logout(request, userId, ipAddress);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Usuario actual", description = "Retorna información del usuario autenticado")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
