package com.pos.system.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SaleRequest {

    private Long clientId;

    @Size(max = 500)
    private String notes;

    /** Código de cupón opcional */
    private String couponCode;

    /** Puntos a canjear (opcional) */
    private Long puntosCanje;

    @NotNull(message = "Los items son obligatorios")
    @Size(min = 1, message = "Debe haber al menos un item")
    private List<SaleItemRequest> items;

    @NotNull(message = "Los pagos son obligatorios")
    @Size(min = 1, message = "Debe haber al menos un método de pago")
    private List<PaymentRequest> payments;

    @Data
    public static class SaleItemRequest {

        @NotNull(message = "El ID del producto es obligatorio")
        private Long productId;

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a 0")
        private Integer quantity;

        private BigDecimal discount;
    }

    @Data
    public static class PaymentRequest {

        @NotNull(message = "El método de pago es obligatorio")
        private String paymentMethod;

        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a 0")
        private BigDecimal amount;

        private String reference;
    }
}
