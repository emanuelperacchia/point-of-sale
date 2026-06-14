package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "sales_targets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesTarget {

    public enum TipoBono {
        FIJO, PORCENTAJE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private Integer mes;

    @Column(nullable = false)
    private Integer anio;

    @Column(name = "meta_monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal metaMonto;

    @Enumerated(EnumType.STRING)
    @Column(name = "bono_por_superacion", nullable = false, length = 20)
    private TipoBono bonoPorSuperacion;

    @Column(name = "valor_bono", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorBono;
}
