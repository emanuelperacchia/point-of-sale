package com.pos.system.dto.response;

import com.pos.system.entity.Payroll;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollResponse {
    private Long id;
    private Long employeeId;
    private Integer mes;
    private Integer anio;
    private Integer diasTrabajados;
    private Integer horasNormalesMinutos;
    private Integer horasExtraMinutos;
    private BigDecimal sueldoBasico;
    private BigDecimal plusHorasExtra;
    private BigDecimal comisiones;
    private BigDecimal bonoDesempeno;
    private BigDecimal totalHaberes;
    private BigDecimal descJubilacion;
    private BigDecimal descObraSocial;
    private BigDecimal descAnses;
    private BigDecimal descAusencias;
    private BigDecimal descEmbargos;
    private BigDecimal totalDescuentos;
    private BigDecimal netoApagar;
    private Payroll.Estado estado;
    private Long aprobadoPor;
    private LocalDate fechaAprobacion;
    private LocalDateTime createdAt;
}
