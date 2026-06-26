package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WebhookDeliveryResponse {

    private Long id;
    private Long webhookId;
    private String evento;
    private String payload;
    private Integer statusCode;
    private Integer intentos;
    private LocalDateTime ultimoIntento;
    private String estado;
    private LocalDateTime createdAt;
}
