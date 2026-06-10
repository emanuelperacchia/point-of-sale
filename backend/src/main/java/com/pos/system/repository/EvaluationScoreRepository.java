package com.pos.system.repository;

import com.pos.system.entity.EvaluationScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationScoreRepository extends JpaRepository<EvaluationScore, Long> {

    List<EvaluationScore> findByEvaluationId(Long evaluationId);
}
