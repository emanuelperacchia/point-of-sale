package com.pos.system.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransferRequest {

    @NotNull(message = "sucursalOrigenId es requerido")
    private Long sucursalOrigenId;

    @NotNull(message = "sucursalDestinoId es requerido")
    private Long sucursalDestinoId;

    private String motivo;

    @NotEmpty(message = "Debe haber al menos un item")
    private List<TransferItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferItem {
        @NotNull(message = "productId es requerido")
        private Long productId;

        @NotNull(message = "cantidad es requerida")
        private Integer cantidad;
    }
}
