package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ecommerce_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcommerceConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Column(name = "api_key", nullable = false, length = 500)
    private String apiKey;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "sync_frequency_minutes", nullable = false)
    @Builder.Default
    private Integer syncFrequencyMinutes = 5;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "ultima_sync")
    private LocalDateTime ultimaSync;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = null;

    @UpdateTimestamp
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = null;
}
