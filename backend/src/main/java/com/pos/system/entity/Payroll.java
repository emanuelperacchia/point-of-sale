package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payroll")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payroll {

    public enum Estado {
        BORRADOR, APROBADA
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

    @Column(name = "dias_trabajados")
    private Integer diasTrabajados;

    @Column(name = "horas_normales_minutos")
    private Integer horasNormalesMinutos;

    @Column(name = "horas_extra_minutos")
    private Integer horasExtraMinutos;

    @Column(name = "sueldo_basico", precision = 12, scale = 2)
    private BigDecimal sueldoBasico;

    @Column(name = "plus_horas_extra", precision = 12, scale = 2)
    private BigDecimal plusHorasExtra;

    @Column(precision = 12, scale = 2)
    private BigDecimal comisiones;

    @Column(name = "bono_desempeno", precision = 12, scale = 2)
    private BigDecimal bonoDesempeno;

    @Column(name = "total_haberes", precision = 12, scale = 2)
    private BigDecimal totalHaberes;

    @Column(name = "desc_jubilacion", precision = 12, scale = 2)
    private BigDecimal descJubilacion;

    @Column(name = "desc_obra_social", precision = 12, scale = 2)
    private BigDecimal descObraSocial;

    @Column(name = "desc_anses", precision = 12, scale = 2)
    private BigDecimal descAnses;

    @Column(name = "desc_ausencias", precision = 12, scale = 2)
    private BigDecimal descAusencias;

    @Column(name = "desc_embargos", precision = 12, scale = 2)
    private BigDecimal descEmbargos;

    @Column(name = "total_descuentos", precision = 12, scale = 2)
    private BigDecimal totalDescuentos;

    @Column(name = "neto_a_pagar", precision = 12, scale = 2)
    private BigDecimal netoApagar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Estado estado = Estado.BORRADOR;

    @Column(name = "aprobado_por")
    private Long aprobadoPor;

    @Column(name = "fecha_aprobacion")
    private LocalDate fechaAprobacion;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
