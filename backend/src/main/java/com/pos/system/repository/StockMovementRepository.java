package com.pos.system.repository;

import com.pos.system.entity.Product;
import com.pos.system.entity.StockMovement;
import com.pos.system.entity.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    
    // Kardex por producto
    Page<StockMovement> findByProductOrderByCreatedAtDesc(
        Product product, 
        Pageable pageable
    );
    
    // Kardex por producto y bodega
    Page<StockMovement> findByProductAndWarehouseOrderByCreatedAtDesc(
        Product product, 
        Warehouse warehouse, 
        Pageable pageable
    );
    
    // Movimientos por rango de fechas
    List<StockMovement> findByCreatedAtBetween(
        LocalDateTime startDate, 
        LocalDateTime endDate
    );
    
    // Últimos movimientos
    List<StockMovement> findTop10ByOrderByCreatedAtDesc();
    
    // Movimientos por tipo
    List<StockMovement> findByType(StockMovement.MovementType type);
    
    // Query para reporte de movimientos
    @Query("SELECT sm FROM StockMovement sm " +
           "WHERE sm.product.id = :productId " +
           "AND sm.warehouse.id = :warehouseId " +
           "AND sm.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY sm.createdAt DESC")
    List<StockMovement> findMovementsByProductAndDateRange(
        @Param("productId") Long productId,
        @Param("warehouseId") Long warehouseId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}