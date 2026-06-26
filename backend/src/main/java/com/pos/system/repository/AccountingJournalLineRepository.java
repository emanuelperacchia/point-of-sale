package com.pos.system.repository;

import com.pos.system.entity.AccountingJournalLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface AccountingJournalLineRepository extends JpaRepository<AccountingJournalLine, Long> {

    @Query("SELECT l FROM AccountingJournalLine l " +
           "JOIN FETCH l.cuenta c " +
           "WHERE l.entry.fecha = :fecha " +
           "ORDER BY c.codigo, l.tipo")
    List<AccountingJournalLine> findByEntryFecha(@Param("fecha") LocalDate fecha);

    @Query("SELECT l.cuenta.id AS cuentaId, l.cuenta.codigo AS codigo, l.cuenta.nombre AS nombre, " +
           "SUM(CASE WHEN l.tipo = 'DEBE' THEN l.monto ELSE 0 END) AS totalDebe, " +
           "SUM(CASE WHEN l.tipo = 'HABER' THEN l.monto ELSE 0 END) AS totalHaber " +
           "FROM AccountingJournalLine l " +
           "WHERE l.entry.fecha <= :hasta " +
           "GROUP BY l.cuenta.id, l.cuenta.codigo, l.cuenta.nombre " +
           "ORDER BY l.cuenta.codigo")
    List<Object[]> calculateTrialBalanceRaw(@Param("hasta") LocalDate hasta);
}
