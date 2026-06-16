package com.pos.system.dto.response;

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
public class LoteTraceabilityResponse {

    private String numeroLote;
    private LocalDate fechaProduccion;
    private Integer cantidad;
    private String productoTerminadoNombre;
    private Long productionOrderId;
    private List<MateriaPrimaUsada> materiasPrimas;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MateriaPrimaUsada {
        private String productoNombre;
        private String productoSku;
        private BigDecimal cantidad;
        private String loteCompra;
        private String proveedor;
    }
}
