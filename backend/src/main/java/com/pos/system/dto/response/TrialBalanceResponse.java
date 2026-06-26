package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrialBalanceResponse {

    private LocalDate fecha;
    private List<TrialBalanceRow> cuentas;
    private Totales totales;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrialBalanceRow {
        private Long cuentaId;
        private String codigo;
        private String nombre;
        private BigDecimal debe;
        private BigDecimal haber;
        private BigDecimal saldo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Totales {
        private BigDecimal totalDebe;
        private BigDecimal totalHaber;
        private BigDecimal diferencia;
    }
}
