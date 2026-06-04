package com.pos.system.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class ValidateCartDiscountsRequest {
    private List<CartItemDto> items;
    private String couponCode;

    @Data
    public static class CartItemDto {
        private Long productId;
        private String productName;
        private Integer quantity;
        private java.math.BigDecimal unitPrice;
        private Long categoryId;
    }
}
