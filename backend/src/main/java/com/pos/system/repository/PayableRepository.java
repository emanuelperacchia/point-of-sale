package com.pos.system.repository;

import com.pos.system.entity.Payable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PayableRepository extends JpaRepository<Payable, Long> {

    List<Payable> findBySupplierId(Long supplierId);

    List<Payable> findByEstado(Payable.Estado estado);

    @Query("SELECT p FROM Payable p WHERE p.fechaVencimiento < :hoy AND p.estado IN ('PENDIENTE','PARCIAL')")
    List<Payable> findVencidas(@Param("hoy") LocalDate hoy);

    @Query("SELECT p FROM Payable p WHERE p.fechaVencimiento BETWEEN :desde AND :hasta AND p.estado IN ('PENDIENTE','PARCIAL')")
    List<Payable> findProximosVencer(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("SELECT p FROM Payable p WHERE p.estado IN ('PENDIENTE','PARCIAL','VENCIDA') " +
           "AND (:supplierId IS NULL OR p.supplierId = :supplierId) " +
           "AND (:estado IS NULL OR p.estado = :estado) " +
           "AND (:fechaVencimiento IS NULL OR p.fechaVencimiento <= :fechaVencimiento)")
    List<Payable> findByFilters(@Param("supplierId") Long supplierId,
                                @Param("estado") Payable.Estado estado,
                                @Param("fechaVencimiento") LocalDate fechaVencimiento);

    @Query("SELECT COALESCE(SUM(p.saldoPendiente), 0) FROM Payable p " +
           "WHERE p.fechaVencimiento BETWEEN :desde AND :hasta AND p.estado IN ('PENDIENTE','PARCIAL')")
    java.math.BigDecimal totalPendienteEntreFechas(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
