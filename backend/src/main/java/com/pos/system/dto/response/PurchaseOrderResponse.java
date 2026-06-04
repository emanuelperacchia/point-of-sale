package com.pos.system.dto.response;

import com.pos.system.entity.PurchaseOrder;
import com.pos.system.entity.PurchaseOrderDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO de salida con los datos completos de una orden de compra.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderResponse {

    private Long id;
    private String orderNumber;
    private String supplierName;
    private String supplierTaxId;
    private String warehouseName;
    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private LocalDate deliveryDate;
    private String status;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal total;
    private String paymentTerm;
    private String notes;
    private String contactPerson;
    private String createdByName;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private Boolean isOverdue;
    private List<PurchaseOrderDetailResponse> details;

    /**
     * DTO para cada línea de detalle de la orden de compra.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PurchaseOrderDetailResponse {

        private Long id;
        private String productCode;
        private String productName;
        private BigDecimal quantity;
        private BigDecimal quantityReceived;
        private BigDecimal pendingQuantity;
        private BigDecimal unitPrice;
        private BigDecimal discountPercentage;
        private BigDecimal taxAmount;
        private BigDecimal subtotal;
        private String notes;
    }

    /**
     * Construye un PurchaseOrderResponse a partir de la entidad.
     */
    public static PurchaseOrderResponse fromEntity(PurchaseOrder order) {
        return PurchaseOrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .supplierName(order.getSupplier().getBusinessName())
                .supplierTaxId(order.getSupplier().getTaxId())
                .warehouseName(order.getWarehouse().getName())
                .orderDate(order.getOrderDate())
                .expectedDeliveryDate(order.getExpectedDeliveryDate())
                .deliveryDate(order.getDeliveryDate())
                .status(order.getStatus().getDescription())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .taxAmount(order.getTaxAmount())
                .total(order.getTotal())
                .paymentTerm(order.getPaymentTerm().getDescription())
                .notes(order.getNotes())
                .contactPerson(order.getContactPerson())
                .createdByName(order.getCreatedBy() != null
                        ? order.getCreatedBy().getFullName() : null)
                .approvedByName(order.getApprovedBy() != null
                        ? order.getApprovedBy().getFullName() : null)
                .approvedAt(order.getApprovedAt())
                .createdAt(order.getCreatedAt())
                .isOverdue(order.isOverdue())
                .details(order.getDetails().stream()
                        .map(PurchaseOrderResponse::detailFromEntity)
                        .collect(Collectors.toList()))
                .build();
    }

    private static PurchaseOrderDetailResponse detailFromEntity(PurchaseOrderDetail detail) {
        return PurchaseOrderDetailResponse.builder()
                .id(detail.getId())
                .productCode(detail.getProduct().getSku())
                .productName(detail.getProduct().getName())
                .quantity(detail.getQuantity())
                .quantityReceived(detail.getQuantityReceived())
                .pendingQuantity(detail.getPendingQuantity())
                .unitPrice(detail.getUnitPrice())
                .discountPercentage(detail.getDiscountPercentage())
                .taxAmount(detail.getTaxAmount())
                .subtotal(detail.getSubtotal())
                .notes(detail.getNotes())
                .build();
    }
}
