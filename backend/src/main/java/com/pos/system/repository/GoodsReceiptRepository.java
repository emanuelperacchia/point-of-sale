package com.pos.system.repository;

import com.pos.system.entity.GoodsReceipt;
import com.pos.system.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para recepciones de mercadería.
 */
@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {

    Optional<GoodsReceipt> findByReceiptNumber(String receiptNumber);

    List<GoodsReceipt> findByPurchaseOrder(PurchaseOrder purchaseOrder);

    boolean existsByReceiptNumber(String receiptNumber);

    /**
     * Obtiene el siguiente número correlativo de recepción para un prefijo dado.
     */
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(g.receiptNumber, 4) AS integer)), 0) "
            + "FROM GoodsReceipt g WHERE g.receiptNumber LIKE :prefix")
    Integer getNextReceiptNumber(@Param("prefix") String prefix);
}
