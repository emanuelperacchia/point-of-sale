package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "points_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointsTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long clientId;

    @Column
    private Long saleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PointTransactionType tipo;

    @Column(nullable = false)
    private Long puntos;

    @Column(nullable = false)
    private Long saldoPrevio;

    @Column(nullable = false)
    private Long saldoPosterior;

    @Column(length = 255)
    private String descripcion;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;
}
