package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ecommerce_sync_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcommerceSyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_id", nullable = false)
    private Long configId;

    @Column(nullable = false, length = 20)
    private String tipo; // CATALOG, STOCK, ORDERS

    @Column(nullable = false, length = 10)
    private String resultado; // OK, ERROR

    @Column(length = 1000)
    private String mensaje;

    @Column(name = "items_processed", nullable = false)
    @Builder.Default
    private Integer itemsProcessed = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime timestamp = null;
}
