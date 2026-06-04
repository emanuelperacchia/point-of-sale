package com.pos.system.dto.request;

import com.pos.system.entity.Supplier;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de entrada para crear o actualizar un proveedor.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierRequest {

    @NotBlank(message = "El código es obligatorio")
    @Size(max = 50, message = "El código no puede exceder 50 caracteres")
    private String code;

    @NotBlank(message = "El RUT/CUIT es obligatorio")
    @Size(max = 20, message = "El RUT/CUIT no puede exceder 20 caracteres")
    private String taxId;

    @NotBlank(message = "La razón social es obligatoria")
    @Size(max = 200, message = "La razón social no puede exceder 200 caracteres")
    private String businessName;

    @Size(max = 200, message = "El nombre comercial no puede exceder 200 caracteres")
    private String tradeName;

    @Size(max = 255, message = "La dirección no puede exceder 255 caracteres")
    private String address;

    @Size(max = 50)
    private String city;

    @Size(max = 50)
    private String state;

    @Size(max = 20)
    private String postalCode;

    @Size(max = 50)
    private String country;

    @Size(max = 20)
    private String phone;

    @Email(message = "Email inválido")
    @Size(max = 100)
    private String email;

    @Size(max = 100)
    private String website;

    private Long categoryId;

    @NotNull(message = "El término de pago es obligatorio")
    private Supplier.PaymentTerm paymentTerm;

    @DecimalMin(value = "0.0", message = "El descuento debe ser mayor o igual a 0")
    @DecimalMax(value = "100.0", message = "El descuento no puede exceder 100%")
    private BigDecimal discountPercentage;

    @DecimalMin(value = "0.0", message = "El límite de crédito debe ser mayor o igual a 0")
    private BigDecimal creditLimit;

    @Size(max = 500)
    private String notes;

    @NotNull(message = "El estado activo es obligatorio")
    private Boolean active;
}
