package com.pos.system.dto.response;

import com.pos.system.entity.Employee;
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
public class EmployeeResponse {

    private Long id;
    private String nombre;
    private String apellido;
    private String dni;
    private String cuil;
    private LocalDate fechaNacimiento;
    private LocalDate fechaIngreso;
    private LocalDate fechaBaja;
    private String cargo;
    private String departamento;
    private Long sucursalId;
    private BigDecimal salarioBase;
    private Employee.ModalidadContrato modalidadContrato;
    private Long userId;
    private Boolean activo;
    private String documentoUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
