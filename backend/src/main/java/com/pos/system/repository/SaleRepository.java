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
            WHERE s.status = 'COMPLETED'
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

    // ── Dashboard Ejecutivo ─────────────────────────────────────────

    long countByStatusAndCreatedAtBetween(SaleStatus status, LocalDateTime desde, LocalDateTime hasta);

    @Query("SELECT COALESCE(SUM(s.total), 0) FROM Sale s WHERE s.status = 'COMPLETED' AND s.createdAt >= :desde AND s.createdAt < :hasta")
    BigDecimal sumTotalByCreatedAtBetween(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query(value = """
            SELECT p.id, p.name, p.sku, COALESCE(SUM(si.quantity * si.unit_price), 0), COALESCE(SUM(si.quantity), 0)
            FROM sale_items si
            JOIN products p ON p.id = si.product_id
            JOIN sales s ON s.id = si.sale_id
            WHERE s.status = 'COMPLETED'
              AND s.created_at >= :desde
              AND s.created_at < :hasta
            GROUP BY p.id, p.name, p.sku
            ORDER BY COALESCE(SUM(si.quantity * si.unit_price), 0) DESC
            LIMIT 5
            """, nativeQuery = true)
    List<Object[]> findTopProducts(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query(value = """
            SELECT u.id, COALESCE(CONCAT(e.nombre, ' ', e.apellido), u.username),
                   COALESCE(SUM(s.total), 0), COUNT(DISTINCT s.id)
            FROM sales s
            JOIN users u ON u.id = s.user_id
            LEFT JOIN employees e ON e.user_id = u.id
            WHERE s.status = 'COMPLETED'
              AND s.created_at >= :desde
              AND s.created_at < :hasta
            GROUP BY u.id, e.nombre, e.apellido, u.username
            ORDER BY COALESCE(SUM(s.total), 0) DESC
            LIMIT 5
            """, nativeQuery = true)
    List<Object[]> findTopSellers(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    // ── Reportes Avanzados de Ventas (US-036) ──────────────────────

    @Query("SELECT COALESCE(SUM(s.total), 0) FROM Sale s WHERE s.status = 'REFUNDED' AND s.createdAt >= :desde AND s.createdAt < :hasta")
    BigDecimal sumRefundsByCreatedAtBetween(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COALESCE(SUM(s.discount), 0) FROM Sale s WHERE s.status = 'COMPLETED' AND s.createdAt >= :desde AND s.createdAt < :hasta")
    BigDecimal sumDiscountsByCreatedAtBetween(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COALESCE(SUM(s.taxAmount), 0) FROM Sale s WHERE s.status = 'COMPLETED' AND s.createdAt >= :desde AND s.createdAt < :hasta")
    BigDecimal sumTaxesByCreatedAtBetween(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query(value = """
            SELECT p.payment_method, COALESCE(SUM(p.amount), 0), COUNT(DISTINCT p.sale_id)
            FROM payments p
            JOIN sales s ON s.id = p.sale_id
            WHERE s.status = 'COMPLETED'
              AND s.created_at >= :desde
              AND s.created_at < :hasta
            GROUP BY p.payment_method
            ORDER BY COALESCE(SUM(p.amount), 0) DESC
            """, nativeQuery = true)
    List<Object[]> findSalesByPaymentMethod(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query(value = """
            SELECT EXTRACT(HOUR FROM s.created_at) AS hora,
                   COALESCE(SUM(s.total), 0), COUNT(*)
            FROM sales s
            WHERE s.status = 'COMPLETED'
              AND s.created_at >= :desde
              AND s.created_at < :hasta
            GROUP BY EXTRACT(HOUR FROM s.created_at)
            ORDER BY hora
            """, nativeQuery = true)
    List<Object[]> findSalesByHour(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query(value = """
            SELECT EXTRACT(DOW FROM s.created_at) AS dia_numero,
                   COALESCE(SUM(s.total), 0), COUNT(*)
            FROM sales s
            WHERE s.status = 'COMPLETED'
              AND s.created_at >= :desde
              AND s.created_at < :hasta
            GROUP BY EXTRACT(DOW FROM s.created_at)
            ORDER BY dia_numero
            """, nativeQuery = true)
    List<Object[]> findSalesByDayOfWeek(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    // ── Análisis de Productos (US-037) ─────────────────────────────

    @Query(value = """
            SELECT p.id, p.name, p.sku, COALESCE(c.name, 'Sin categoría'),
                   COALESCE(SUM(si.quantity * si.unit_price), 0), COALESCE(SUM(si.quantity), 0)
            FROM sale_items si
            JOIN products p ON p.id = si.product_id
            LEFT JOIN categories c ON c.id = p.category_id
            JOIN sales s ON s.id = si.sale_id
            WHERE s.status = 'COMPLETED'
              AND s.created_at >= :desde
              AND s.created_at < :hasta
            GROUP BY p.id, p.name, p.sku, c.name
            ORDER BY COALESCE(SUM(si.quantity * si.unit_price), 0) DESC
            """, nativeQuery = true)
    List<Object[]> findAllProductSales(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query(value = """
            SELECT p.id, p.name, p.sku
            FROM products p
            WHERE p.active = true
              AND p.id NOT IN (
                  SELECT DISTINCT si.product_id
                  FROM sale_items si
                  JOIN sales s ON s.id = si.sale_id
                  WHERE s.status = 'COMPLETED'
                    AND s.created_at >= :desde
              )
            """, nativeQuery = true)
    List<Object[]> findProductsWithoutSalesSince(@Param("desde") LocalDateTime desde);
}
