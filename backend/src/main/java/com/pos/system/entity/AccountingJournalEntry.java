package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "accounting_journal_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountingJournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(name = "referencia_id", nullable = false)
    private Long referenciaId;

    @Column(name = "referencia_type", nullable = false, length = 20)
    private String referenciaType;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "GENERADO";

    @Column(name = "webhook_enviado", nullable = false)
    @Builder.Default
    private Boolean webhookEnviado = false;

    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AccountingJournalLine> lineas;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = null;
}
