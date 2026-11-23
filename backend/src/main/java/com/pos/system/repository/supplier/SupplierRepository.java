package com.pos.system.repository.supplier;

import com.pos.system.entity.supplier.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findByCode(String code);

    List<Supplier> findByBusinessNameContainingIgnoreCase(String businessName);

    List<Supplier> findByActiveTrue();

    boolean existsByCode(String code);
}
