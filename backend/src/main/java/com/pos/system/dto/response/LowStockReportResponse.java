package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LowStockReportResponse {

    private Long totalProductsBelowMinimum;
    private LocalDateTime generatedAt;
    private List<LowStockItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LowStockItem {
        private Long productId;
        private String productName;
        private String productSku;
        private Long warehouseId;
        private String warehouseName;
        private BigDecimal currentStock;
        private BigDecimal minimumStock;
        private BigDecimal suggestedPurchase;
    }
}