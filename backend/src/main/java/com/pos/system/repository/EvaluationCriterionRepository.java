package com.pos.system.repository;

import com.pos.system.entity.EvaluationCriterion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationCriterionRepository extends JpaRepository<EvaluationCriterion, Long> {

    List<EvaluationCriterion> findByTemplateId(Long templateId);

    List<EvaluationCriterion> findByTemplateIdIn(List<Long> templateIds);
}
