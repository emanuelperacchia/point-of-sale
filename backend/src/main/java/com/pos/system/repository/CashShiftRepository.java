package com.pos.system.repository;

import com.pos.system.entity.CashShift;
import com.pos.system.entity.ShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CashShiftRepository extends JpaRepository<CashShift, Long> {

    Optional<CashShift> findByCajeroIdAndEstado(Long cajeroId, ShiftStatus estado);

    @Query("SELECT cs FROM CashShift cs WHERE " +
           "(:cajeroId IS NULL OR cs.cajero.id = :cajeroId) AND " +
           "(:estado IS NULL OR cs.estado = :estado) " +
           "ORDER BY cs.fechaApertura DESC")
    List<CashShift> findByFilters(
            @Param("cajeroId") Long cajeroId,
            @Param("estado") ShiftStatus estado);
}
