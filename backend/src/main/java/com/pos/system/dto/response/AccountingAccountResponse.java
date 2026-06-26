package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountingAccountResponse {

    private Long id;
    private String codigo;
    private String nombre;
    private String tipo;
    private Long cuentaPadreId;
    private String cuentaPadreNombre;
    private Integer nivel;
    private Boolean activa;
    private LocalDateTime createdAt;
}
