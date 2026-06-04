package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartValidationResponse {

    private boolean valid;
    private List<ItemStatus> items;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ItemStatus {
        private Long productId;
        private String productName;
        private int requested;
        private int available;
        private boolean enoughStock;
        private String message;
    }
}
