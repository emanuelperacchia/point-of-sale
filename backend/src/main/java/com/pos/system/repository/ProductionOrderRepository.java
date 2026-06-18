package com.pos.system.repository;

import com.pos.system.entity.ProductionOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, Long> {
    List<ProductionOrder> findByEstado(ProductionOrder.Estado estado);
    List<ProductionOrder> findBySucursalId(Long sucursalId);
    List<ProductionOrder> findByRecipeId(Long recipeId);

    long countByEstadoAndCreatedAtBetween(ProductionOrder.Estado estado, LocalDateTime desde, LocalDateTime hasta);
}
