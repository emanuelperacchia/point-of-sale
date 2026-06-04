package com.pos.system.dto.response;

import com.pos.system.entity.Supplier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de salida con los datos completos de un proveedor.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierResponse {

    private Long id;
    private String code;
    private String taxId;
    private String businessName;
    private String tradeName;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String phone;
    private String email;
    private String website;
    private String categoryName;
    private String paymentTerm;
    private Integer paymentDays;
    private BigDecimal discountPercentage;
    private BigDecimal creditLimit;
    private BigDecimal currentDebt;
    private BigDecimal availableCredit;
    private BigDecimal rating;
    private Boolean active;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Construye un SupplierResponse a partir de la entidad Supplier.
     */
    public static SupplierResponse fromEntity(Supplier supplier) {
        return SupplierResponse.builder()
                .id(supplier.getId())
                .code(supplier.getCode())
                .taxId(supplier.getTaxId())
                .businessName(supplier.getBusinessName())
                .tradeName(supplier.getTradeName())
                .address(supplier.getAddress())
                .city(supplier.getCity())
                .state(supplier.getState())
                .postalCode(supplier.getPostalCode())
                .country(supplier.getCountry())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .website(supplier.getWebsite())
                .categoryName(supplier.getCategory() != null
                        ? supplier.getCategory().getName() : null)
                .paymentTerm(supplier.getPaymentTerm().getDescription())
                .paymentDays(supplier.getPaymentTerm().getDays())
                .discountPercentage(supplier.getDiscountPercentage())
                .creditLimit(supplier.getCreditLimit())
                .currentDebt(supplier.getCurrentDebt())
                .availableCredit(supplier.getCreditLimit().subtract(supplier.getCurrentDebt()))
                .rating(supplier.getRating())
                .active(supplier.getActive())
                .notes(supplier.getNotes())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }
}
