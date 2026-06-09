package com.pos.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateExpenseFromStatementRequest {
    @NotNull
    private Long statementId;

    @NotNull
    private BigDecimal monto;

    @NotBlank
    private String categoria;

    @NotBlank
    private String descripcion;

    private LocalDate fecha;
}
