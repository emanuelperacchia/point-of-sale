package com.pos.system.dto.request;

import com.pos.system.entity.Employee;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class EmployeeRequest {

    @NotBlank
    private String nombre;

    @NotBlank
    private String apellido;

    @NotBlank
    private String dni;

    private String cuil;

    @NotNull
    private LocalDate fechaNacimiento;

    @NotNull
    private LocalDate fechaIngreso;

    @NotBlank
    private String cargo;

    @NotBlank
    private String departamento;

    private Long sucursalId;

    @NotNull
    @Positive
    private BigDecimal salarioBase;

    @NotNull
    private Employee.ModalidadContrato modalidadContrato;

    private Long userId;
}
