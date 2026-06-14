package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "commission_schemes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionScheme {

    public enum Tipo {
        PORCENTAJE_VENTA, MONTO_FIJO_POR_VENTA, ESCALONADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Tipo tipo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "vigencia_desde", nullable = false)
    private LocalDate vigenciaDesde;

    @Column(name = "vigencia_hasta")
    private LocalDate vigenciaHasta;

    @Column(precision = 5, scale = 2)
    private BigDecimal valor; // for PORCENTAJE_VENTA or MONTO_FIJO_POR_VENTA
}
