package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Certificado digital para firmar XML de comprobantes electrónicos.
 * <p>
 * Almacena el binario del archivo PKCS#12 (.p12) o JKS, su contraseña
 * (encriptada en base de datos), vigencia y metadatos del emisor.
 * </p>
 */
@Entity
@Table(name = "digital_certificates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DigitalCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String alias;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String storeType = "PKCS12";

    @Column(nullable = false, length = 500)
    private String storePassword;

    @Lob
    @Column(nullable = false)
    private byte[] certificateData;

    @Column(nullable = false)
    private LocalDateTime validFrom;

    @Column(nullable = false)
    private LocalDateTime validTo;

    @Column(length = 255)
    private String issuer;

    @Column(length = 255)
    private String subject;

    @Column(length = 100)
    private String serialNumber;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
