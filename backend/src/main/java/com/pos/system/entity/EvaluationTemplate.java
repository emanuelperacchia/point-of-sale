package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "evaluation_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationTemplate {

    public enum Periodo {
        MENSUAL, TRIMESTRAL, SEMESTRAL, ANUAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Periodo periodo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
