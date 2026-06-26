package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BranchPriceResponse {

    private Long id;
    private Long branchId;
    private Long productId;
    private String productName;
    private String productSku;
    private BigDecimal precio;
    private BigDecimal precioGlobal;
    private LocalDate vigenciaDesde;
    private LocalDate vigenciaHasta;
    private Boolean activo;
    private LocalDateTime createdAt;
}
