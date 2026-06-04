package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Detalle de una recepción de mercadería.
 * Cada línea registra la cantidad recibida, dañada o faltante de un producto,
 * así como su costo unitario y datos de lote/vencimiento.
 */
@Entity
@Table(name = "goods_receipt_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_receipt_id", nullable = false)
    private GoodsReceipt goodsReceipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_detail_id", nullable = false)
    private PurchaseOrderDetail purchaseOrderDetail;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal expectedQuantity;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal receivedQuantity = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal damagedQuantity = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal missingQuantity = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal unitCost;

    @Column(length = 50)
    private String batchNumber;

    private LocalDate expirationDate;

    @Column(length = 255)
    private String notes;

    // -- Transient (calculated) --

    @Transient
    public BigDecimal getAcceptedQuantity() {
        return receivedQuantity.subtract(damagedQuantity);
    }

    @Transient
    public BigDecimal getTotalCost() {
        return getAcceptedQuantity().multiply(unitCost);
    }
}
