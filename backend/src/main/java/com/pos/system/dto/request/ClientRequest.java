package com.pos.system.dto.request;

import com.pos.system.entity.CondicionIva;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClientRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    private String name;

    @Size(max = 20)
    private String documentType;

    @Size(max = 50)
    private String documentNumber;

    @Email(message = "Email inválido")
    @Size(max = 150)
    private String email;

    @Size(max = 20)
    private String phone;

    @Size(max = 255)
    private String address;

    // Campos fiscales
    @Size(max = 200)
    private String businessName;

    private CondicionIva condicionIva;

    @Size(max = 255)
    private String taxAddress;
}
