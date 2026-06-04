package com.pos.system.repository;

import com.pos.system.entity.SupplierCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la gestión de categorías de proveedores.
 */
@Repository
public interface SupplierCategoryRepository extends JpaRepository<SupplierCategory, Long> {

    Optional<SupplierCategory> findByCode(String code);

    boolean existsByCode(String code);

    List<SupplierCategory> findByActiveTrue();
}
