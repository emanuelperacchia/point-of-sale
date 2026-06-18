package com.pos.system.repository;

import com.pos.system.entity.ProductionOrderComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductionOrderComponentRepository extends JpaRepository<ProductionOrderComponent, Long> {
    List<ProductionOrderComponent> findByProductionOrderId(Long productionOrderId);
    void deleteByProductionOrderId(Long productionOrderId);

    @Query(value = """
            SELECT COALESCE(AVG(poc.merma_real), 0)
            FROM production_order_components poc
            JOIN production_orders po ON po.id = poc.production_order_id
            WHERE po.estado = 'COMPLETADA'
              AND po.created_at >= :desde
              AND po.created_at < :hasta
            """, nativeQuery = true)
    List<Object[]> findAverageWasteByPeriod(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
