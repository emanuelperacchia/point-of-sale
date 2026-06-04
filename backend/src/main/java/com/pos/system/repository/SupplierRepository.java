package com.pos.system.repository;

import com.pos.system.entity.Supplier;
import com.pos.system.entity.SupplierCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la gestión de proveedores.
 */
@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findByCode(String code);

    Optional<Supplier> findByTaxId(String taxId);

    boolean existsByCode(String code);

    boolean existsByTaxId(String taxId);

    List<Supplier> findByActiveTrue();

    List<Supplier> findByCategory(SupplierCategory category);

    /**
     * Búsqueda por razón social o nombre comercial.
     */
    @Query("SELECT s FROM Supplier s WHERE "
            + "LOWER(s.businessName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
            + "LOWER(s.tradeName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Supplier> searchByName(@Param("searchTerm") String searchTerm);

    /**
     * Proveedores con deuda pendiente, ordenados por monto adeudado descendente.
     */
    @Query("SELECT s FROM Supplier s WHERE s.currentDebt > 0 ORDER BY s.currentDebt DESC")
    List<Supplier> findSuppliersWithDebt();

    /**
     * Proveedores cercanos al 80% de su límite de crédito.
     */
    @Query("SELECT s FROM Supplier s WHERE "
            + "s.creditLimit > 0 AND "
            + "(s.currentDebt / s.creditLimit) > 0.8")
    List<Supplier> findSuppliersNearCreditLimit();

    /**
     * Proveedores activos ordenados por rating descendente.
     */
    @Query("SELECT s FROM Supplier s WHERE s.active = true ORDER BY s.rating DESC")
    List<Supplier> findTopRatedSuppliers();
}
