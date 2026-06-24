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
public class StockTransferResponse {

    private Long id;
    private Long sucursalOrigenId;
    private Long sucursalDestinoId;
    private String estado;
    private String motivo;
    private Long solicitadoPor;
    private Long despachadoPor;
    private Long recibidoPor;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaDespacho;
    private LocalDateTime fechaRecepcion;
    private List<TransferItemResponse> items;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TransferItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private Integer cantidadSolicitada;
        private Integer cantidadDespachada;
        private Integer cantidadRecibida;
    }
}
