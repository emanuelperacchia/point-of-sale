package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_deliveries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookDelivery {

    public enum Estado {
        PENDIENTE, ENTREGADO, FALLIDO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "webhook_id", nullable = false)
    private Long webhookId;

    @Column(nullable = false, length = 100)
    private String evento;

    @Column(columnDefinition = "JSON")
    private String payload;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(nullable = false)
    @Builder.Default
    private Integer intentos = 0;

    @Column(name = "ultimo_intento")
    private LocalDateTime ultimoIntento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Estado estado = Estado.PENDIENTE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
