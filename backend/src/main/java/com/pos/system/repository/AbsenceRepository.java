package com.pos.system.repository;

import com.pos.system.entity.Absence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {

    List<Absence> findByEmployeeIdAndFechaBetweenOrderByFechaAsc(Long employeeId, LocalDate desde, LocalDate hasta);

    @Query("SELECT COUNT(a) FROM Absence a WHERE a.employeeId = :employeeId AND a.tipo = :tipo AND YEAR(a.fecha) = :anio AND MONTH(a.fecha) = :mes")
    int countByEmployeeIdAndTipoAndMes(@Param("employeeId") Long employeeId, @Param("tipo") Absence.Tipo tipo, @Param("mes") int mes, @Param("anio") int anio);
}
