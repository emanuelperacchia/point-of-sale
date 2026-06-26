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
public class ApiKeyUsageResponse {

    private Long totalRequests;
    private Long requestsToday;
    private LocalDateTime ultimoUso;
    private Long errores4xx;
    private Long errores5xx;
}
