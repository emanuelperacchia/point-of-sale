package com.pos.system.repository;

import com.pos.system.entity.BranchPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BranchPriceRepository extends JpaRepository<BranchPrice, Long> {

    Optional<BranchPrice> findByBranchIdAndProductIdAndActivoTrue(Long branchId, Long productId);

    List<BranchPrice> findByBranchIdAndActivoTrue(Long branchId);

    @Query("SELECT bp FROM BranchPrice bp WHERE bp.branchId = :branchId AND bp.activo = true " +
           "AND (bp.vigenciaDesde IS NULL OR bp.vigenciaDesde <= :fecha) " +
           "AND (bp.vigenciaHasta IS NULL OR bp.vigenciaHasta >= :fecha)")
    List<BranchPrice> findVigentesByBranchId(@Param("branchId") Long branchId, @Param("fecha") LocalDate fecha);

    @Query("SELECT bp FROM BranchPrice bp WHERE bp.branchId = :branchId AND bp.productId = :productId AND bp.activo = true " +
           "AND (bp.vigenciaDesde IS NULL OR bp.vigenciaDesde <= :fecha) " +
           "AND (bp.vigenciaHasta IS NULL OR bp.vigenciaHasta >= :fecha)")
    Optional<BranchPrice> findVigente(@Param("branchId") Long branchId, @Param("productId") Long productId,
                                       @Param("fecha") LocalDate fecha);

    @Modifying
    @Query("UPDATE BranchPrice bp SET bp.activo = false WHERE bp.branchId = :branchId AND bp.activo = true")
    int desactivarTodosPorBranch(@Param("branchId") Long branchId);

    @Modifying
    @Query("UPDATE BranchPrice bp SET bp.activo = false WHERE bp.vigenciaHasta < :fecha AND bp.activo = true")
    int desactivarVencidos(@Param("fecha") LocalDate fecha);

    @Query("SELECT bp FROM BranchPrice bp JOIN Product p ON p.id = bp.productId " +
           "WHERE bp.branchId = :branchId AND bp.activo = true AND p.active = true")
    List<BranchPrice> findActivosConProducto(@Param("branchId") Long branchId);
}
