package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "accounting_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountingAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20, unique = true)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tipo tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_padre_id")
    private AccountingAccount cuentaPadre;

    @Column(nullable = false)
    @Builder.Default
    private Integer nivel = 1;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = null;

    @UpdateTimestamp
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = null;

    public enum Tipo {
        ACTIVO, PASIVO, PATRIMONIO, INGRESO, EGRESO
    }
}
