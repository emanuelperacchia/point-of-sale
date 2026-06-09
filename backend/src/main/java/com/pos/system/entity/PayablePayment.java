package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payable_payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayablePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payable_id", nullable = false)
    private Long payableId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 20)
    private PaymentMethod metodoPago;

    @Column(length = 100)
    private String referenciaBancaria;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "registrado_por", nullable = false)
    private Long registradoPor;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
