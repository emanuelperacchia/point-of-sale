package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "commission_tiers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scheme_id", nullable = false)
    private Long schemeId;

    @Column(name = "monto_desde", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoDesde;

    @Column(name = "monto_hasta", precision = 12, scale = 2)
    private BigDecimal montoHasta; // nullable = último tramo

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;
}
