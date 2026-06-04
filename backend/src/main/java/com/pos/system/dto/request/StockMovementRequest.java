package com.pos.system.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovementRequest {

    @NotNull(message = "El producto es requerido")
    private Long productId;

    @NotNull(message = "La bodega es requerida")
    private Long warehouseId;

    @NotNull(message = "La cantidad es requerida")
    @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a 0")
    private BigDecimal quantity;

    private BigDecimal unitCost;

    private String reason;

    private String referenceDocument;
}