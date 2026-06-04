package com.pos.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de entrada para crear o actualizar una categoría de proveedor.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierCategoryRequest {

    @NotBlank(message = "El código es obligatorio")
    @Size(max = 50, message = "El código no puede exceder 50 caracteres")
    private String code;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String name;

    @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
    private String description;

    private Boolean active;
}
