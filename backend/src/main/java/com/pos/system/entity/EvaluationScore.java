package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "evaluation_scores")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evaluation_id", nullable = false)
    private Long evaluationId;

    @Column(name = "criterion_id", nullable = false)
    private Long criterionId;

    @Column(nullable = false)
    private Integer puntuacion; // 1-5
}
