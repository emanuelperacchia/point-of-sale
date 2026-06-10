package com.pos.system.repository;

import com.pos.system.entity.EvaluationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationTemplateRepository extends JpaRepository<EvaluationTemplate, Long> {

    List<EvaluationTemplate> findByActivoTrue();
}
