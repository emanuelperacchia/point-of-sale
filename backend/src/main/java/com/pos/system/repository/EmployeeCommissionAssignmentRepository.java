package com.pos.system.repository;

import com.pos.system.entity.EmployeeCommissionAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface EmployeeCommissionAssignmentRepository extends JpaRepository<EmployeeCommissionAssignment, Long> {

    @Query("SELECT a FROM EmployeeCommissionAssignment a WHERE a.employeeId = :employeeId AND a.vigenciaDesde <= :fecha AND (a.vigenciaHasta IS NULL OR a.vigenciaHasta >= :fecha)")
    Optional<EmployeeCommissionAssignment> findVigente(@Param("employeeId") Long employeeId, @Param("fecha") LocalDate fecha);
}
