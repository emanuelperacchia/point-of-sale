package com.pos.system.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReceiveTransferRequest {

    @NotEmpty(message = "Debe haber al menos un item recibido")
    private List<ReceivedItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceivedItem {
        private Long productId;
        private Integer cantidadRecibida;
    }
}
