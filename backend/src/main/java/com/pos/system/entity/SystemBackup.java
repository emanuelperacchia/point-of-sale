package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_backups")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemBackup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(nullable = false, length = 500)
    private String ubicacion;

    @Column(name = "tipo_storage", nullable = false, length = 20)
    @Builder.Default
    private String tipoStorage = "LOCAL";

    @Column(name = "creado_por")
    private Long creadoPor;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
