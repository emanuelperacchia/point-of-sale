package com.pos.system.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Email es requerido")
    @Email(message = "Email debe ser válido")
    private String email;

    @NotBlank(message = "Password es requerido")
    @Size(min = 8, message = "Password debe tener al menos 8 caracteres")
    private String password;

    @NotBlank(message = "Nombre es requerido")
    private String firstName;

    @NotBlank(message = "Apellido es requerido")
    private String lastName;

    private String phone;
}
