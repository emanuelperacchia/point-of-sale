package com.pos.system.dto.request;

import com.pos.system.entity.SupplierReturn;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de entrada para registrar una devolución a proveedor.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierReturnRequest {

    @NotNull(message = "El proveedor es obligatorio")
    private Long supplierId;

    private Long purchaseOrderId;

    @NotNull(message = "El producto es obligatorio")
    private Long productId;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a 0")
    private BigDecimal quantity;

    @NotNull(message = "El costo unitario es obligatorio")
    @DecimalMin(value = "0.0", message = "El costo unitario debe ser mayor o igual a 0")
    private BigDecimal unitCost;

    @NotNull(message = "La fecha de devolución es obligatoria")
    private LocalDate returnDate;

    @NotNull(message = "El motivo es obligatorio")
    private SupplierReturn.ReturnReason reason;

    @NotNull(message = "La bodega es obligatoria")
    private Long warehouseId;

    @Size(max = 500)
    private String notes;
}
