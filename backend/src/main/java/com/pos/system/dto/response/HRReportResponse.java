package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Reportes de RRHH (US-040).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HRReportResponse {

    private ResumenRRHH resumen;
    private List<ProductividadVendedor> productividad;
    private String status;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResumenRRHH {
        private long empleadosActivos;
        private BigDecimal costoLaboralMensual;
        private BigDecimal salarioPromedio;
        private double ausentismoPct;
        private long ausenciasMes;
        private long totalEmpleadosEsperados;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductividadVendedor {
        private Long employeeId;
        private String nombre;
        private String cargo;
        private BigDecimal ventasPeriodo;
        private Long transacciones;
        private BigDecimal ventasPorHora;
        private BigDecimal comisiones;
    }
}
