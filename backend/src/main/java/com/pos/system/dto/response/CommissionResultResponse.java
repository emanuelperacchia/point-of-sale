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
public class CommissionResultResponse {

    private Long id;
    private Long employeeId;
    private Integer mes;
    private Integer anio;
    private BigDecimal totalVentas;
    private BigDecimal comisionCalculada;
    private Boolean metaAlcanzada;
    private BigDecimal bonoAplicado;
    private String esquemaUsado;
}
