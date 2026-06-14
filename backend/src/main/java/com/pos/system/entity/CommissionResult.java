package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "commission_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private Integer mes;

    @Column(nullable = false)
    private Integer anio;

    @Column(name = "total_ventas", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalVentas;

    @Column(name = "comision_calculada", nullable = false, precision = 12, scale = 2)
    private BigDecimal comisionCalculada;

    @Column(name = "meta_alcanzada")
    private Boolean metaAlcanzada;

    @Column(name = "bono_aplicado", precision = 12, scale = 2)
    private BigDecimal bonoAplicado;

    @Column(name = "esquema_usado", length = 200)
    private String esquemaUsado;
}
