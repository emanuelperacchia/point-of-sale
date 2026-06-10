package com.pos.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEvaluationRequest {

    @NotNull
    private Long employeeId;

    @NotNull
    private Long templateId;

    @NotBlank
    private String periodo;

    @NotNull
    private LocalDate fechaEvaluacion;

    private String observaciones;

    @NotNull
    private List<ScoreRequest> scores;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScoreRequest {
        @NotNull
        private Long criterionId;

        @NotNull
        private Integer puntuacion;
    }
}
