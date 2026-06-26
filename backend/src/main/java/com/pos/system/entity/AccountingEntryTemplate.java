package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "accounting_entry_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountingEntryTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "evento_origen", nullable = false, length = 30, unique = true)
    private EventoOrigen eventoOrigen;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("orden ASC")
    private List<AccountingEntryTemplateLine> lineas;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = null;

    @UpdateTimestamp
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = null;

    public enum EventoOrigen {
        VENTA, COMPRA, GASTO, NOMINA
    }
}
