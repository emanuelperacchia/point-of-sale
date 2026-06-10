package com.pos.system.repository;

import com.pos.system.entity.PerformanceEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerformanceEvaluationRepository extends JpaRepository<PerformanceEvaluation, Long> {

    List<PerformanceEvaluation> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<PerformanceEvaluation> findByEstadoOrderByCreatedAtDesc(PerformanceEvaluation.Estado estado);
}
