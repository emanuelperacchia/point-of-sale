package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "key_hash", nullable = false, length = 64, unique = true)
    private String keyHash;

    @Column(name = "key_prefix", nullable = false, length = 20)
    private String keyPrefix;

    @Column(nullable = false, length = 500)
    @Builder.Default
    private String permisos = "";

    @Column(name = "rate_limit", nullable = false)
    @Builder.Default
    private Integer rateLimit = 60;

    @Column(name = "expiracion")
    private LocalDateTime expiracion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "creado_por")
    private Long creadoPor;

    @Column(name = "ultimo_uso")
    private LocalDateTime ultimoUso;

    @Column(name = "total_requests", nullable = false)
    @Builder.Default
    private Long totalRequests = 0L;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public boolean hasPermission(String permission) {
        if (permisos == null || permisos.isBlank()) return false;
        return permisos.contains(permission);
    }

    public boolean isExpirada() {
        return expiracion != null && LocalDateTime.now().isAfter(expiracion);
    }
}
