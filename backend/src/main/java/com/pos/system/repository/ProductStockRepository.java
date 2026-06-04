package com.pos.system.repository;

import com.pos.system.entity.Product;
import com.pos.system.entity.ProductStock;
import com.pos.system.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductStockRepository extends JpaRepository<ProductStock, Long> {
    
    // Stock por producto y bodega
    Optional<ProductStock> findByProductAndWarehouse(Product product, Warehouse warehouse);
    
    // Todos los stocks de un producto
    List<ProductStock> findByProduct(Product product);
    
    // Productos bajo stock mínimo
    @Query("SELECT ps FROM ProductStock ps " +
           "WHERE ps.currentStock < ps.minimumStock " +
           "AND ps.warehouse.active = true")
    List<ProductStock> findProductsBelowMinimum();
    
    // Productos bajo stock mínimo en bodega específica
    @Query("SELECT ps FROM ProductStock ps " +
           "WHERE ps.currentStock < ps.minimumStock " +
           "AND ps.warehouse.id = :warehouseId")
    List<ProductStock> findProductsBelowMinimumByWarehouse(@Param("warehouseId") Long warehouseId);
    
    // Stock total de un producto (todas las bodegas)
    @Query("SELECT SUM(ps.currentStock) FROM ProductStock ps " +
           "WHERE ps.product.id = :productId " +
           "AND ps.warehouse.active = true")
    BigDecimal getTotalStockByProduct(@Param("productId") Long productId);
    
    // Productos sin stock
    @Query("SELECT ps FROM ProductStock ps " +
           "WHERE ps.currentStock = 0 " +
           "AND ps.warehouse.active = true")
    List<ProductStock> findProductsWithoutStock();
    
    // Valorización total del inventario
    @Query("SELECT SUM(ps.totalValue) FROM ProductStock ps " +
           "WHERE ps.warehouse.id = :warehouseId")
    BigDecimal getTotalInventoryValue(@Param("warehouseId") Long warehouseId);
}