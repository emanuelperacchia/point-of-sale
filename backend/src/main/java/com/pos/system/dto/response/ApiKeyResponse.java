package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiKeyResponse {

    private Long id;
    private String nombre;
    private String keyPrefix;
    private String apiKey; // solo visible al crear
    private List<String> permisos;
    private Integer rateLimit;
    private LocalDateTime expiracion;
    private Boolean activo;
    private LocalDateTime ultimoUso;
    private Long totalRequests;
    private LocalDateTime createdAt;
}
