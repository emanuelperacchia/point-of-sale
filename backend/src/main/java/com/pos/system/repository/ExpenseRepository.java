package com.pos.system.repository;

import com.pos.system.entity.Expense;
import com.pos.system.entity.Expense.ExpenseCategory;
import com.pos.system.entity.Expense.ExpenseEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByCategoria(ExpenseCategory categoria);

    List<Expense> findByEstado(ExpenseEstado estado);

    List<Expense> findByFechaBetween(LocalDate desde, LocalDate hasta);

    List<Expense> findByCategoriaAndFechaBetween(ExpenseCategory categoria, LocalDate desde, LocalDate hasta);

    @Query("SELECT e FROM Expense e WHERE " +
           "(:categoria IS NULL OR e.categoria = :categoria) AND " +
           "(:estado IS NULL OR e.estado = :estado) AND " +
           "(:desde IS NULL OR e.fecha >= :desde) AND " +
           "(:hasta IS NULL OR e.fecha <= :hasta) AND " +
           "(:proveedorId IS NULL OR e.proveedorId = :proveedorId)")
    List<Expense> findByFilters(
            @Param("categoria") ExpenseCategory categoria,
            @Param("estado") ExpenseEstado estado,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("proveedorId") Long proveedorId);

    /**
     * Gastos recurrentes cuya próxima fecha de generación es hoy.
     */
    @Query("SELECT e FROM Expense e WHERE e.recurrente = true AND e.proximaFecha = :fecha")
    List<Expense> findRecurrentesVencidos(@Param("fecha") LocalDate fecha);

    /**
     * Suma de gastos PAGADO por día para flujo de caja.
     */
    @Query(value = """
            SELECT e.fecha, COALESCE(SUM(e.monto), 0)
            FROM expenses e
            WHERE e.estado = 'PAGADO'
              AND e.fecha >= :desde
              AND e.fecha < :hasta
            GROUP BY e.fecha
            ORDER BY e.fecha
            """, nativeQuery = true)
    List<Object[]> findDailyExpenseTotals(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    /**
     * Suma de gastos por categoría en un período.
     */
    @Query(value = """
            SELECT e.categoria, COALESCE(SUM(e.monto), 0)
            FROM expenses e
            WHERE e.estado = 'PAGADO'
              AND e.fecha >= :desde
              AND e.fecha < :hasta
            GROUP BY e.categoria
            ORDER BY e.categoria
            """, nativeQuery = true)
    List<Object[]> findTotalsByCategoria(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);
}
