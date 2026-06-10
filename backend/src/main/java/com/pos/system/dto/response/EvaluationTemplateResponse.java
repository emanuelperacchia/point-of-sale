package com.pos.system.dto.response;

import com.pos.system.entity.EvaluationTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationTemplateResponse {

    private Long id;
    private String nombre;
    private EvaluationTemplate.Periodo periodo;
    private Boolean activo;
    private List<CriterionResponse> criterios;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CriterionResponse {
        private Long id;
        private String nombre;
        private BigDecimal peso;
    }
}
