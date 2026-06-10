package com.pos.system.dto.request;

import com.pos.system.entity.EvaluationTemplate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationTemplateRequest {

    @NotBlank
    private String nombre;

    @NotNull
    private EvaluationTemplate.Periodo periodo;

    @NotNull
    private List<CriterionRequest> criterios;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CriterionRequest {
        @NotBlank
        private String nombre;

        @NotNull
        private java.math.BigDecimal peso;
    }
}
