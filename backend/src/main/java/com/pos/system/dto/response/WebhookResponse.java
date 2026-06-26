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
public class WebhookResponse {

    private Long id;
    private String url;
    private List<String> eventos;
    private String secreto;
    private Boolean activo;
    private LocalDateTime createdAt;
}
