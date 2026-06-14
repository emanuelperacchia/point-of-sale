package com.pos.system.dto.request;

import com.pos.system.entity.CommissionScheme;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CommissionSchemeRequest {

    @NotBlank
    private String nombre;

    @NotNull
    private CommissionScheme.Tipo tipo;

    @NotNull
    private LocalDate vigenciaDesde;

    private LocalDate vigenciaHasta;

    private List<TierRequest> tiers; // required for ESCALONADO
    private BigDecimal valor; // for PORCENTAJE_VENTA or MONTO_FIJO_POR_VENTA

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TierRequest {
        @NotNull
        private BigDecimal montoDesde;
        private BigDecimal montoHasta;
        @NotNull
        private BigDecimal porcentaje;
    }
}
