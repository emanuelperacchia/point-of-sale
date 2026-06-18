package com.pos.system.repository;

import com.pos.system.entity.Receivable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReceivableRepository extends JpaRepository<Receivable, Long> {

    List<Receivable> findByClientId(Long clientId);

    List<Receivable> findByEstado(Receivable.Estado estado);

    List<Receivable> findByClientIdAndEstado(Long clientId, Receivable.Estado estado);

    @Query("SELECT r FROM Receivable r WHERE r.fechaVencimiento < :hoy AND r.estado IN ('PENDIENTE','PARCIAL')")
    List<Receivable> findVencidas(@Param("hoy") LocalDate hoy);

    @Query("SELECT r FROM Receivable r WHERE r.estado IN ('PENDIENTE','PARCIAL','VENCIDA') " +
           "AND (:clienteId IS NULL OR r.clientId = :clienteId) " +
           "AND (:estado IS NULL OR r.estado = :estado) " +
           "AND (:fechaVencimiento IS NULL OR r.fechaVencimiento <= :fechaVencimiento)")
    List<Receivable> findByFilters(@Param("clienteId") Long clienteId,
                                   @Param("estado") Receivable.Estado estado,
                                   @Param("fechaVencimiento") LocalDate fechaVencimiento);

    @Query("SELECT r FROM Receivable r WHERE r.estado IN ('PENDIENTE','PARCIAL','VENCIDA') " +
           "ORDER BY r.fechaVencimiento ASC")
    List<Receivable> findAllActive();

    boolean existsByClientIdAndEstadoIn(Long clientId, List<Receivable.Estado> estados);

    @Query("SELECT COALESCE(SUM(r.saldoPendiente), 0) FROM Receivable r WHERE r.estado IN ('PENDIENTE', 'PARCIAL', 'VENCIDA')")
    BigDecimal sumOverdueReceivables();
}
