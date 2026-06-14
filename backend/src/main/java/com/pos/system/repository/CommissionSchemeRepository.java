package com.pos.system.repository;

import com.pos.system.entity.CommissionScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CommissionSchemeRepository extends JpaRepository<CommissionScheme, Long> {

    List<CommissionScheme> findByActivoTrue();

    @Query("SELECT s FROM CommissionScheme s WHERE s.activo = true AND s.vigenciaDesde <= :fecha AND (s.vigenciaHasta IS NULL OR s.vigenciaHasta >= :fecha)")
    List<CommissionScheme> findVigentes(@Param("fecha") LocalDate fecha);
}
