package com.pos.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class WebhookRequest {

    @NotBlank(message = "La URL es obligatoria")
    @Size(max = 500, message = "La URL no puede exceder 500 caracteres")
    private String url;

    private List<String> eventos;

    private Boolean activo;
}
