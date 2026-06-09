package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgingReportResponse {

    private ResumenGeneral resumenGeneral;
    private java.util.List<TramoCliente> porCliente;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResumenGeneral {
        private BigDecimal corriente;
        private BigDecimal tramo1a30;
        private BigDecimal tramo31a60;
        private BigDecimal tramo61a90;
        private BigDecimal masDe90;
        private BigDecimal total;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TramoCliente {
        private Long clientId;
        private String clientName;
        private String clientDocument;
        private BigDecimal corriente;
        private BigDecimal tramo1a30;
        private BigDecimal tramo31a60;
        private BigDecimal tramo61a90;
        private BigDecimal masDe90;
        private BigDecimal total;
    }
}
