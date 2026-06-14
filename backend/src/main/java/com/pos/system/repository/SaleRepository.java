package com.pos.system.repository;

import com.pos.system.entity.Sale;
import com.pos.system.entity.SaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    Page<Sale> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Sale> findByClientIdOrderByCreatedAtDesc(Long clientId, Pageable pageable);

    Page<Sale> findByStatusOrderByCreatedAtDesc(SaleStatus status, Pageable pageable);

    long countByStatus(SaleStatus status);

    /**
     * Suma de totales de ventas COMPLETADA por día para flujo de caja.
     */
    @Query(value = """
            SELECT CAST(s.created_at AS DATE) AS dia, COALESCE(SUM(s.total), 0)
            FROM sales s
            WHERE s.status = 'COMPLETADA'
              AND s.created_at >= :desde
              AND s.created_at < :hasta
            GROUP BY CAST(s.created_at AS DATE)
            ORDER BY dia
            """, nativeQuery = true)
    List<Object[]> findDailySalesTotals(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COALESCE(SUM(s.total), 0) FROM Sale s WHERE s.user.id = :userId AND s.status = 'COMPLETED' AND s.createdAt >= :desde AND s.createdAt < :hasta")
    BigDecimal sumTotalByUserAndDateRange(@Param("userId") Long userId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
