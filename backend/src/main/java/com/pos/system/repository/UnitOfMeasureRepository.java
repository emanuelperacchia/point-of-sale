package com.pos.system.repository;

import com.pos.system.entity.UnitOfMeasure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnitOfMeasureRepository extends JpaRepository<UnitOfMeasure, Long> {

    Optional<UnitOfMeasure> findBySymbol(String symbol);

    Optional<UnitOfMeasure> findByName(String name);

    boolean existsBySymbol(String symbol);

    boolean existsByName(String name);

    List<UnitOfMeasure> findByActiveTrue();

    List<UnitOfMeasure> findByCategory(UnitOfMeasure.UnitCategory category);

    List<UnitOfMeasure> findByBaseUnitTrue();
}