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
public class AlertResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private Long warehouseId;
    private String warehouseName;
    private String type;
    private String severity;
    private String message;
    private BigDecimal currentStock;
    private BigDecimal minimumStock;
    private Boolean read;
    private Boolean resolved;
    private LocalDateTime resolvedAt;
    private String resolvedBy;
    private LocalDateTime createdAt;
}