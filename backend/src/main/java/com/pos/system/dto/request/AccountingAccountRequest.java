package com.pos.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AccountingAccountRequest {

    @NotBlank
    @Pattern(regexp = "^[0-9]+(\\.[0-9]+)*$", message = "El código debe ser numérico con niveles separados por punto, ej: 1.1.1")
    @Size(max = 20)
    private String codigo;

    @NotBlank
    @Size(max = 200)
    private String nombre;

    @NotNull
    private String tipo; // ACTIVO, PASIVO, PATRIMONIO, INGRESO, EGRESO

    private Long cuentaPadreId;

    private Integer nivel;

    private Boolean activa;
}
