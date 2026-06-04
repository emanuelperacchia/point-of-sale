package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "clients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 20)
    private String documentType;

    @Column(length = 50)
    private String documentNumber;

    @Column(length = 150)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String address;

    // --- Campos fiscales (para facturación electrónica) ---

    /** Razón social (para Factura A/B/C) */
    @Column(length = 200)
    private String businessName;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CondicionIva condicionIva = CondicionIva.CONSUMIDOR_FINAL;

    /** Domicilio fiscal (puede diferir del domicilio particular) */
    @Column(length = 255)
    private String taxAddress;

    // --- Campos de fidelización (US-017) ---

    @Column(nullable = false)
    @Builder.Default
    private Long puntosAcumulados = 0L;

    @Column(length = 10)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ClientTier tier = ClientTier.BRONCE;

    @Column
    private LocalDate fechaUltimaTransaccion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
