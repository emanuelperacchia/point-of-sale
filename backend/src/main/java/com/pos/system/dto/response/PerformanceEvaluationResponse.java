package com.pos.system.dto.response;

import com.pos.system.entity.PerformanceEvaluation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceEvaluationResponse {

    private Long id;
    private Long employeeId;
    private Long templateId;
    private String templateName;
    private String periodo;
    private LocalDate fechaEvaluacion;
    private BigDecimal puntuacionFinal;
    private String observaciones;
    private PerformanceEvaluation.Estado estado;
    private Long evaluadoPor;
    private List<ScoreResponse> scores;
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScoreResponse {
        private Long id;
        private Long criterionId;
        private String criterionName;
        private Integer puntuacion;
    }
}
