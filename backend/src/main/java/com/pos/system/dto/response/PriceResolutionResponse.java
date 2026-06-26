package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PriceResolutionResponse {

    private Long productId;
    private String productName;
    private String productSku;
    private BigDecimal precioFinal;
    private BigDecimal precioGlobal;
    private BigDecimal precioLocal;
    private Boolean esLocal;
    private Long sucursalId;
    private LocalDate vigenciaHasta;
}
