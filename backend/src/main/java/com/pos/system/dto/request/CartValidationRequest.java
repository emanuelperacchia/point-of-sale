package com.pos.system.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CartValidationRequest {

    @NotNull
    private Long warehouseId;

    @NotNull
    @jakarta.validation.constraints.Size(min = 1)
    private List<CartItem> items;

    @Data
    public static class CartItem {
        @NotNull
        private Long productId;

        @NotNull
        @Positive
        private Integer quantity;
    }

    public record ItemValidation(Long productId, Integer quantity) {}
}
