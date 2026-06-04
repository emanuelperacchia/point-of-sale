package com.pos.system.dto.response;

import com.pos.system.entity.CondicionIva;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientResponse {

    private Long id;
    private String name;
    private String documentType;
    private String documentNumber;
    private String email;
    private String phone;
    private String address;

    // Campos fiscales
    private String businessName;
    private CondicionIva condicionIva;

    private String taxAddress;
    private Boolean active;
    private LocalDateTime createdAt;
}
