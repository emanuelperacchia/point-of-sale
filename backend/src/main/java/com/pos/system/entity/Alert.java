package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlertType type;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AlertSeverity severity = AlertSeverity.MEDIUM;

    @Column(length = 500)
    private String message;

    @Column(nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal currentStock;

    @Column(nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal minimumStock;

    @Column(nullable = false)
    @Builder.Default
    private Boolean read = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean resolved = false;

    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum AlertType {
        STOCK_BAJO,
        STOCK_CRITICO,
        STOCK_AGOTADO,
        STOCK_EXCESO,
        PRODUCTO_INACTIVO
    }

    public enum AlertSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}