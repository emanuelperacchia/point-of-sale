package com.pos.system.repository;

import com.pos.system.entity.ShiftDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShiftDefinitionRepository extends JpaRepository<ShiftDefinition, Long> {

    List<ShiftDefinition> findByActivoTrue();
}
