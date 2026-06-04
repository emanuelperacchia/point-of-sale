package com.pos.system.repository;

import com.pos.system.entity.Tax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaxRepository extends JpaRepository<Tax, Long> {

    Optional<Tax> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByName(String name);

    List<Tax> findByActiveTrue();

    List<Tax> findByType(Tax.TaxType type);

    List<Tax> findByRegion(String region);

    List<Tax> findByActiveTrueAndRegion(String region);
}