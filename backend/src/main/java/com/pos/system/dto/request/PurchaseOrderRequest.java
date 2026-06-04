package com.pos.system.dto.request;

import com.pos.system.entity.Supplier;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO de entrada para crear o actualizar una orden de compra.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderRequest {

    @NotNull(message = "El proveedor es obligatorio")
    private Long supplierId;

    @NotNull(message = "La bodega es obligatoria")
    private Long warehouseId;

    @NotNull(message = "La fecha de orden es obligatoria")
    private LocalDate orderDate;

    private LocalDate expectedDeliveryDate;

    @NotNull(message = "El término de pago es obligatorio")
    private Supplier.PaymentTerm paymentTerm;

    @DecimalMin(value = "0.0", message = "El descuento debe ser mayor o igual a 0")
    private BigDecimal discountAmount;

    @DecimalMin(value = "0.0", message = "El impuesto debe ser mayor o igual a 0")
    private BigDecimal taxAmount;

    @Size(max = 500, message = "Las notas no pueden exceder 500 caracteres")
    private String notes;

    @Size(max = 100)
    private String contactPerson;

    @NotEmpty(message = "Debe incluir al menos un producto")
    @Valid
    private List<PurchaseOrderDetailRequest> details;

    /**
     * DTO para cada línea de detalle de la orden de compra.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PurchaseOrderDetailRequest {

        @NotNull(message = "El producto es obligatorio")
        private Long productId;

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a 0")
        private BigDecimal quantity;

        @NotNull(message = "El precio unitario es obligatorio")
        @DecimalMin(value = "0.0", message = "El precio debe ser mayor o igual a 0")
        private BigDecimal unitPrice;

        @DecimalMin(value = "0.0", message = "El descuento debe ser mayor o igual a 0")
        @DecimalMax(value = "100.0", message = "El descuento no puede exceder 100%")
        private BigDecimal discountPercentage;

        @DecimalMin(value = "0.0")
        private BigDecimal taxAmount;

        @Size(max = 255)
        private String notes;
    }
}
