package com.pos.system.repository;

import com.pos.system.entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PayrollRepository extends JpaRepository<Payroll, Long> {

    List<Payroll> findByEmployeeIdAndAnio(Long employeeId, Integer anio);
    List<Payroll> findByMesAndAnio(Integer mes, Integer anio);
    Optional<Payroll> findByEmployeeIdAndMesAndAnio(Long employeeId, Integer mes, Integer anio);

    @Query("SELECT COALESCE(SUM(p.netoApagar), 0) FROM Payroll p WHERE p.estado = 'APROBADA' AND p.mes = :mes AND p.anio = :anio")
    BigDecimal sumNetoByMesAndAnio(@Param("mes") Integer mes, @Param("anio") Integer anio);

    @Query("SELECT COUNT(DISTINCT p.employeeId) FROM Payroll p WHERE p.mes = :mes AND p.anio = :anio")
    long countDistinctEmployeeByMesAndAnio(@Param("mes") Integer mes, @Param("anio") Integer anio);
}