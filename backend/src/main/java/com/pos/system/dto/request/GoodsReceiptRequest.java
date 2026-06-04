package com.pos.system.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO de entrada para registrar una recepción de mercadería.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptRequest {

    @NotNull(message = "La orden de compra es obligatoria")
    private Long purchaseOrderId;

    @NotNull(message = "La fecha de recepción es obligatoria")
    private LocalDate receiptDate;

    @Size(max = 500)
    private String notes;

    @NotEmpty(message = "Debe incluir al menos un detalle")
    @Valid
    private List<GoodsReceiptDetailRequest> details;

    /**
     * DTO para cada línea de recepción.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GoodsReceiptDetailRequest {

        @NotNull(message = "El detalle de orden de compra es obligatorio")
        private Long purchaseOrderDetailId;

        @NotNull(message = "El producto es obligatorio")
        private Long productId;

        @NotNull(message = "La cantidad esperada es obligatoria")
        @DecimalMin(value = "0.01", message = "La cantidad esperada debe ser mayor a 0")
        private BigDecimal expectedQuantity;

        @NotNull(message = "La cantidad recibida es obligatoria")
        @DecimalMin(value = "0.0", message = "La cantidad recibida debe ser mayor o igual a 0")
        private BigDecimal receivedQuantity;

        @DecimalMin(value = "0.0", message = "La cantidad dañada debe ser mayor o igual a 0")
        private BigDecimal damagedQuantity;

        @DecimalMin(value = "0.0", message = "La cantidad faltante debe ser mayor o igual a 0")
        private BigDecimal missingQuantity;

        @NotNull(message = "El costo unitario es obligatorio")
        @DecimalMin(value = "0.0", message = "El costo unitario debe ser mayor o igual a 0")
        private BigDecimal unitCost;

        @Size(max = 50)
        private String batchNumber;

        private LocalDate expirationDate;

        @Size(max = 255)
        private String notes;
    }
}
