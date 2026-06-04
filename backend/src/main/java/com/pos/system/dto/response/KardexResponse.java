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
public class KardexResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private Long warehouseId;
    private String warehouseName;
    private String type;
    private String typeDescription;
    private BigDecimal quantity;
    private BigDecimal previousStock;
    private BigDecimal currentStock;
    private BigDecimal unitCost;
    private String reason;
    private String referenceDocument;
    private String createdBy;
    private LocalDateTime createdAt;
}