package com.pos.system.repository;

import com.pos.system.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    Page<Product> findByActiveTrue(Pageable pageable);
    
    Optional<Product> findByIdAndActiveTrue(Long id);
    
    Page<Product> findByActiveTrueAndNameContainingIgnoreCase(String name, Pageable pageable);
    
    boolean existsByName(String name);

    boolean existsBySku(String sku);

    // POS search: by name, SKU, or category name
    @Query("SELECT p FROM Product p LEFT JOIN p.category c " +
           "WHERE p.active = true AND (" +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY p.name")
    List<Product> searchForPos(@Param("query") String query, Pageable pageable);
}
