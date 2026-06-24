package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConsolidatedReportResponse {

    private TotalesGlobales totales;
    private List<BranchSummary> porSucursal;
    private List<ProductoCritico> productosCriticos;
    private ResumenTransferencias transferencias;
    private String status;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TotalesGlobales {
        private BigDecimal ventas;
        private BigDecimal costoVentas;
        private BigDecimal margenBruto;
        private BigDecimal margenPorcentaje;
        private BigDecimal gastos;
        private BigDecimal rentabilidadNeta;
        private long ordenesProduccion;
        private int sucursalesActivas;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BranchSummary {
        private Long branchId;
        private String branchName;
        private BigDecimal ventas;
        private BigDecimal ticketPromedio;
        private BigDecimal margenBruto;
        private double participacionPorcentaje;
        private long transacciones;
        private BigDecimal ausentismo;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductoCritico {
        private Long productId;
        private String productName;
        private int sucursalesConStockBajo;
        private BigDecimal stockTotal;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResumenTransferencias {
        private long totalTransferencias;
        private BigDecimal montoTotalTransferido;
        private Long sucursalMasEnvia;
        private Long sucursalMasRecibe;
    }
}
