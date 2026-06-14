package com.pos.system.repository;

import com.pos.system.entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayrollRepository extends JpaRepository<Payroll, Long> {
    List<Payroll> findByEmployeeIdAndAnio(Long employeeId, Integer anio);
    List<Payroll> findByMesAndAnio(Integer mes, Integer anio);
    Optional<Payroll> findByEmployeeIdAndMesAndAnio(Long employeeId, Integer mes, Integer anio);
}
