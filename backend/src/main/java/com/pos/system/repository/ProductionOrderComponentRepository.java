package com.pos.system.repository;

import com.pos.system.entity.ProductionOrderComponent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductionOrderComponentRepository extends JpaRepository<ProductionOrderComponent, Long> {
    List<ProductionOrderComponent> findByProductionOrderId(Long productionOrderId);
    void deleteByProductionOrderId(Long productionOrderId);
}
