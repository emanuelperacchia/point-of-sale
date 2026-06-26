package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "accounting_entry_template_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountingEntryTemplateLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private AccountingEntryTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private AccountingAccount cuenta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Tipo tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Formula formula;

    @Column(name = "monto_fijo", precision = 12, scale = 2)
    private BigDecimal montoFijo;

    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;

    public enum Tipo {
        DEBE, HABER
    }

    public enum Formula {
        TOTAL, IVA, NETO, SUELDOS, CARGAS_SOCIALES, FIJO
    }
}
