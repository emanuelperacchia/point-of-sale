package com.pos.system.repository;

import com.pos.system.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    
    Optional<Warehouse> findByCode(String code);
    
    boolean existsByCode(String code);
    
    List<Warehouse> findByActiveTrue();
    
    List<Warehouse> findByType(Warehouse.WarehouseType type);
    
    Optional<Warehouse> findByCodeAndActiveTrue(String code);
}