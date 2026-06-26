package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "accounting_journal_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountingJournalLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private AccountingJournalEntry entry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private AccountingAccount cuenta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Tipo tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    public enum Tipo {
        DEBE, HABER
    }
}
