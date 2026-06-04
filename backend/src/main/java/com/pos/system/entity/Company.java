package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Datos fiscales de la empresa emisora.
 * <p>
 * Esta entidad almacena la razón social, CUIT/RUT, domicilio fiscal y punto
 * de venta registrado ante el organismo fiscal (AFIP/SII).
 * Se espera una única fila activa.
 * </p>
 */
@Entity
@Table(name = "companies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String businessName;

    @Column(length = 200)
    private String tradingName;

    @Column(length = 20)
    private String documentType;

    @Column(length = 50)
    private String documentNumber;

    @Column(length = 255)
    private String taxAddress;

    @Column(length = 100)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(nullable = false)
    @Builder.Default
    private Integer puntoVenta = 1;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
