package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "evaluation_criteria")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal peso;
}
