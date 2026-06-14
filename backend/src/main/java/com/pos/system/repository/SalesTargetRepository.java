package com.pos.system.repository;

import com.pos.system.entity.SalesTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalesTargetRepository extends JpaRepository<SalesTarget, Long> {

    Optional<SalesTarget> findByEmployeeIdAndMesAndAnio(Long employeeId, Integer mes, Integer anio);
}
