package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Recepción de mercadería asociada a una orden de compra.
 * Registra la entrada física de productos al inventario y actualiza automáticamente
 * el stock y los costos promedios ponderados.
 */
@Entity
@Table(name = "goods_receipts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String receiptNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(nullable = false)
    private LocalDate receiptDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(length = 500)
    private String notes;

    @OneToMany(mappedBy = "goodsReceipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GoodsReceiptDetail> details = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // -- Helper methods --

    public void addDetail(GoodsReceiptDetail detail) {
        details.add(detail);
        detail.setGoodsReceipt(this);
    }
}
