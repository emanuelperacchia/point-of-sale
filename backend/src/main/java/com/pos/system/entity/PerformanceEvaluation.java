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
@Table(name = "performance_evaluations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceEvaluation {

    public enum Estado {
        BORRADOR, FINALIZADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(nullable = false, length = 100)
    private String periodo;

    @Column(name = "fecha_evaluacion", nullable = false)
    private LocalDate fechaEvaluacion;

    @Column(name = "puntuacion_final", precision = 5, scale = 2)
    private BigDecimal puntuacionFinal;

    @Column(length = 2000)
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Estado estado = Estado.BORRADOR;

    @Column(name = "evaluado_por")
    private Long evaluadoPor;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
