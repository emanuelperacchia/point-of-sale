package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Reporte avanzado de ventas (US-036).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReportResponse {

    private Resumen resumen;
    private List<SalesByPaymentMethod> porMetodoPago;
    private List<SalesByHour> ventasPorHora;
    private List<SalesByDayOfWeek> ventasPorDiaSemana;
    private List<ComparativaPeriodo> comparativa;
    private String periodo;
    private String periodoAnterior;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Resumen {
        private BigDecimal totalVentas;
        private Long cantidadTransacciones;
        private BigDecimal ticketPromedio;
        private BigDecimal totalDevoluciones;
        private BigDecimal descuentosAplicados;
        private BigDecimal impuestosCobrados;
        private BigDecimal variacionVsPeriodoAnterior;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SalesByPaymentMethod {
        private String metodo;
        private BigDecimal monto;
        private Long cantidad;
        private BigDecimal porcentaje;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SalesByHour {
        private Integer hora;
        private BigDecimal monto;
        private Long cantidad;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SalesByDayOfWeek {
        private String dia;
        private Integer diaNumero;
        private BigDecimal monto;
        private Long cantidad;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ComparativaPeriodo {
        private String fecha;
        private BigDecimal montoActual;
        private BigDecimal montoAnterior;
    }
}
