package com.pos.system.repository;

import com.pos.system.entity.PurchaseOrder;
import com.pos.system.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la gestión de órdenes de compra.
 */
@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    Optional<PurchaseOrder> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    List<PurchaseOrder> findByStatus(PurchaseOrder.OrderStatus status);

    Page<PurchaseOrder> findBySupplier(Supplier supplier, Pageable pageable);

    /**
     * Órdenes pendientes de recepción (enviadas, confirmadas o recibidas parcialmente).
     */
    @Query("SELECT po FROM PurchaseOrder po WHERE "
            + "po.status IN ('ENVIADA', 'CONFIRMADA', 'RECIBIDA_PARCIAL')")
    List<PurchaseOrder> findPendingOrders();

    /**
     * Órdenes vencidas (fecha estimada pasada y no están completas ni canceladas).
     */
    @Query("SELECT po FROM PurchaseOrder po WHERE "
            + "po.expectedDeliveryDate < :currentDate AND "
            + "po.status NOT IN ('RECIBIDA_COMPLETA', 'CANCELADA')")
    List<PurchaseOrder> findOverdueOrders(@Param("currentDate") LocalDate currentDate);

    /**
     * Órdenes en un rango de fechas.
     */
    @Query("SELECT po FROM PurchaseOrder po WHERE "
            + "po.orderDate BETWEEN :startDate AND :endDate "
            + "ORDER BY po.orderDate DESC")
    List<PurchaseOrder> findByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Total de compras realizadas a un proveedor (excluye canceladas).
     */
    @Query("SELECT COALESCE(SUM(po.total), 0) FROM PurchaseOrder po WHERE "
            + "po.supplier.id = :supplierId AND "
            + "po.status != 'CANCELADA'")
    BigDecimal getTotalPurchasesBySupplier(@Param("supplierId") Long supplierId);

    /**
     * Obtiene el siguiente número correlativo de orden para un prefijo dado.
     */
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(po.orderNumber, 4) AS integer)), 0) "
            + "FROM PurchaseOrder po WHERE po.orderNumber LIKE :prefix")
    Integer getNextOrderNumber(@Param("prefix") String prefix);
}
