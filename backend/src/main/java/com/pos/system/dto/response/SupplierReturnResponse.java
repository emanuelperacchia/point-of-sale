package com.pos.system.dto.response;

import com.pos.system.entity.SupplierReturn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de salida con los datos de una devolución a proveedor.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierReturnResponse {

    private Long id;
    private String returnNumber;
    private String supplierName;
    private String supplierTaxId;
    private Long purchaseOrderId;
    private String purchaseOrderNumber;
    private String productCode;
    private String productName;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal subtotal;
    private LocalDate returnDate;
    private String reason;
    private String status;
    private String creditNoteNumber;
    private BigDecimal creditNoteAmount;
    private String warehouseName;
    private String createdByName;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Construye un SupplierReturnResponse a partir de la entidad.
     */
    public static SupplierReturnResponse fromEntity(SupplierReturn supplierReturn) {
        return SupplierReturnResponse.builder()
                .id(supplierReturn.getId())
                .returnNumber(supplierReturn.getReturnNumber())
                .supplierName(supplierReturn.getSupplier().getBusinessName())
                .supplierTaxId(supplierReturn.getSupplier().getTaxId())
                .purchaseOrderId(supplierReturn.getPurchaseOrder() != null
                        ? supplierReturn.getPurchaseOrder().getId() : null)
                .purchaseOrderNumber(supplierReturn.getPurchaseOrder() != null
                        ? supplierReturn.getPurchaseOrder().getOrderNumber() : null)
                .productCode(supplierReturn.getProduct().getSku())
                .productName(supplierReturn.getProduct().getName())
                .quantity(supplierReturn.getQuantity())
                .unitCost(supplierReturn.getUnitCost())
                .subtotal(supplierReturn.getSubtotal())
                .returnDate(supplierReturn.getReturnDate())
                .reason(supplierReturn.getReason().getDescription())
                .status(supplierReturn.getStatus().getDescription())
                .creditNoteNumber(supplierReturn.getCreditNoteNumber())
                .creditNoteAmount(supplierReturn.getCreditNoteAmount())
                .warehouseName(supplierReturn.getWarehouse().getName())
                .createdByName(supplierReturn.getCreatedBy() != null
                        ? supplierReturn.getCreatedBy().getFullName() : null)
                .notes(supplierReturn.getNotes())
                .createdAt(supplierReturn.getCreatedAt())
                .updatedAt(supplierReturn.getUpdatedAt())
                .build();
    }
}
