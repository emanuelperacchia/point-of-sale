package com.pos.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockStatusResponse {
    private Long productId;
    private String productName;
    private String productSku;
    private Long warehouseId;
    private String warehouseName;
    private BigDecimal currentStock;
    private BigDecimal reservedStock;
    private BigDecimal availableStock;
    private BigDecimal minimumStock;
    private BigDecimal maximumStock;
    private BigDecimal averageCost;
    private BigDecimal totalValue;
    private boolean isBelowMinimum;
    private boolean isAboveMaximum;
    private BigDecimal suggestedPurchaseQuantity;
    private LocalDateTime lastMovement;
}