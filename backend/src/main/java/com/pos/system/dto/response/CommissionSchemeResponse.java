package com.pos.system.dto.response;

import com.pos.system.entity.CommissionScheme;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionSchemeResponse {

    private Long id;
    private String nombre;
    private CommissionScheme.Tipo tipo;
    private Boolean activo;
    private LocalDate vigenciaDesde;
    private LocalDate vigenciaHasta;
    private BigDecimal valor;
    private List<TierResponse> tiers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TierResponse {
        private Long id;
        private BigDecimal montoDesde;
        private BigDecimal montoHasta;
        private BigDecimal porcentaje;
    }
}
