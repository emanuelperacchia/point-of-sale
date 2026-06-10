package com.pos.system.repository;

import com.pos.system.entity.ShiftAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, Long> {

    List<ShiftAssignment> findByEmployeeIdAndSemana(Long employeeId, LocalDate semana);

    @Query("SELECT sa FROM ShiftAssignment sa WHERE sa.semana = :semana AND (:sucursalId IS NULL OR sa.sucursalId = :sucursalId)")
    List<ShiftAssignment> findBySemanaAndSucursal(@Param("semana") LocalDate semana, @Param("sucursalId") Long sucursalId);

    List<ShiftAssignment> findByEmployeeId(Long employeeId);

    boolean existsByEmployeeIdAndSemanaAndShiftDefinitionId(Long employeeId, LocalDate semana, Long shiftDefinitionId);
}
