package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ecommerce_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcommerceOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_id", nullable = false)
    private Long configId;

    @Column(name = "external_order_id", nullable = false, length = 100)
    private String externalOrderId;

    @Column(name = "external_data", columnDefinition = "JSON")
    private String externalData;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDIENTE";

    @Column(name = "sale_id")
    private Long saleId;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime importedAt = null;
}
