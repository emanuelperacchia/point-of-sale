package com.pos.system.dto.response;

import com.pos.system.entity.GoodsReceipt;
import com.pos.system.entity.GoodsReceiptDetail;
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
 * DTO de salida con los datos de una recepción de mercadería.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptResponse {

    private Long id;
    private String receiptNumber;
    private Long purchaseOrderId;
    private String purchaseOrderNumber;
    private LocalDate receiptDate;
    private String createdByName;
    private String notes;
    private LocalDateTime createdAt;
    private List<GoodsReceiptDetailResponse> details;

    /**
     * DTO para cada línea de detalle de la recepción.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GoodsReceiptDetailResponse {

        private Long id;
        private String productCode;
        private String productName;
        private BigDecimal expectedQuantity;
        private BigDecimal receivedQuantity;
        private BigDecimal damagedQuantity;
        private BigDecimal missingQuantity;
        private BigDecimal acceptedQuantity;
        private BigDecimal unitCost;
        private BigDecimal totalCost;
        private String batchNumber;
        private LocalDate expirationDate;
        private String notes;
    }

    /**
     * Construye un GoodsReceiptResponse a partir de la entidad.
     */
    public static GoodsReceiptResponse fromEntity(GoodsReceipt receipt) {
        return GoodsReceiptResponse.builder()
                .id(receipt.getId())
                .receiptNumber(receipt.getReceiptNumber())
                .purchaseOrderId(receipt.getPurchaseOrder().getId())
                .purchaseOrderNumber(receipt.getPurchaseOrder().getOrderNumber())
                .receiptDate(receipt.getReceiptDate())
                .createdByName(receipt.getCreatedBy() != null
                        ? receipt.getCreatedBy().getFullName() : null)
                .notes(receipt.getNotes())
                .createdAt(receipt.getCreatedAt())
                .details(receipt.getDetails().stream()
                        .map(GoodsReceiptResponse::detailFromEntity)
                        .collect(Collectors.toList()))
                .build();
    }

    private static GoodsReceiptDetailResponse detailFromEntity(GoodsReceiptDetail detail) {
        return GoodsReceiptDetailResponse.builder()
                .id(detail.getId())
                .productCode(detail.getProduct().getSku())
                .productName(detail.getProduct().getName())
                .expectedQuantity(detail.getExpectedQuantity())
                .receivedQuantity(detail.getReceivedQuantity())
                .damagedQuantity(detail.getDamagedQuantity())
                .missingQuantity(detail.getMissingQuantity())
                .acceptedQuantity(detail.getAcceptedQuantity())
                .unitCost(detail.getUnitCost())
                .totalCost(detail.getTotalCost())
                .batchNumber(detail.getBatchNumber())
                .expirationDate(detail.getExpirationDate())
                .notes(detail.getNotes())
                .build();
    }
}
