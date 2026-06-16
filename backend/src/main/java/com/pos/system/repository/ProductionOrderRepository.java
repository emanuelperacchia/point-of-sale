package com.pos.system.repository;

import com.pos.system.entity.ProductionOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, Long> {
    List<ProductionOrder> findByEstado(ProductionOrder.Estado estado);
    List<ProductionOrder> findBySucursalId(Long sucursalId);
    List<ProductionOrder> findByRecipeId(Long recipeId);
}
