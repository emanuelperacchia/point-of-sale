package com.pos.system.dto.response;

import com.pos.system.entity.SupplierCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de salida con los datos de una categoría de proveedor.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierCategoryResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean active;

    /**
     * Construye un SupplierCategoryResponse a partir de la entidad.
     */
    public static SupplierCategoryResponse fromEntity(SupplierCategory category) {
        return SupplierCategoryResponse.builder()
                .id(category.getId())
                .code(category.getCode())
                .name(category.getName())
                .description(category.getDescription())
                .active(category.getActive())
                .build();
    }
}
