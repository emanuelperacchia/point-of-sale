package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PriceDifferenceReportResponse {

    private Long productId;
    private String productName;
    private String productSku;
    private BigDecimal precioGlobal;
    private BigDecimal precioLocal;
    private BigDecimal diferenciaMonto;
    private BigDecimal diferenciaPorcentaje;
}
