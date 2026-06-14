package com.pos.system.repository;

import com.pos.system.entity.CommissionResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommissionResultRepository extends JpaRepository<CommissionResult, Long> {

    Optional<CommissionResult> findByEmployeeIdAndMesAndAnio(Long employeeId, Integer mes, Integer anio);

    List<CommissionResult> findByMesAndAnioOrderByTotalVentasDesc(Integer mes, Integer anio);
}
