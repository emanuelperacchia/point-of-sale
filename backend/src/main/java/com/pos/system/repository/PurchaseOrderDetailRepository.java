package com.pos.system.repository;

import com.pos.system.entity.Product;
import com.pos.system.entity.PurchaseOrder;
import com.pos.system.entity.PurchaseOrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repositorio para el detalle de órdenes de compra.
 */
@Repository
public interface PurchaseOrderDetailRepository extends JpaRepository<PurchaseOrderDetail, Long> {

    List<PurchaseOrderDetail> findByPurchaseOrder(PurchaseOrder purchaseOrder);

    List<PurchaseOrderDetail> findByProduct(Product product);

    /**
     * Detalles con cantidad pendiente por recibir.
     */
    @Query("SELECT pod FROM PurchaseOrderDetail pod WHERE "
            + "pod.quantity > pod.quantityReceived AND "
            + "pod.purchaseOrder.status IN ('ENVIADA', 'CONFIRMADA', 'RECIBIDA_PARCIAL')")
    List<PurchaseOrderDetail> findPendingDetails();

    /**
     * Cantidad total pendiente de recibir para un producto.
     */
    @Query("SELECT COALESCE(SUM(pod.quantity - pod.quantityReceived), 0) "
            + "FROM PurchaseOrderDetail pod WHERE "
            + "pod.product.id = :productId AND "
            + "pod.purchaseOrder.status IN ('ENVIADA', 'CONFIRMADA', 'RECIBIDA_PARCIAL')")
    BigDecimal getPendingQuantityByProduct(@Param("productId") Long productId);
}
