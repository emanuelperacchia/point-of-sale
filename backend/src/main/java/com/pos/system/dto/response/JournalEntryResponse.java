package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntryResponse {

    private Long id;
    private LocalDate fecha;
    private String descripcion;
    private Long referenciaId;
    private String referenciaType;
    private String estado;
    private LocalDateTime createdAt;
    private List<JournalLineResponse> lineas;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JournalLineResponse {
        private Long id;
        private Long cuentaId;
        private String cuentaCodigo;
        private String cuentaNombre;
        private String tipo;
        private BigDecimal monto;
    }
}
