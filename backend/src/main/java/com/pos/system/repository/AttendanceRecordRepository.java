package com.pos.system.repository;

import com.pos.system.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    Optional<AttendanceRecord> findByEmployeeIdAndFechaAndHoraSalidaIsNull(Long employeeId, LocalDate fecha);

    List<AttendanceRecord> findByEmployeeIdAndFechaBetweenOrderByFechaAsc(Long employeeId, LocalDate desde, LocalDate hasta);

    @Query("SELECT a FROM AttendanceRecord a WHERE a.fecha = :fecha AND a.horaSalida IS NULL")
    List<AttendanceRecord> findSinCheckOutEnFecha(@Param("fecha") LocalDate fecha);

    @Query("SELECT COALESCE(SUM(a.horasTrabajadasMinutos), 0) FROM AttendanceRecord a WHERE a.employeeId = :employeeId AND YEAR(a.fecha) = :anio AND MONTH(a.fecha) = :mes")
    int totalMinutosTrabajados(@Param("employeeId") Long employeeId, @Param("mes") int mes, @Param("anio") int anio);

    @Query("SELECT COALESCE(SUM(a.horasExtraMinutos), 0) FROM AttendanceRecord a WHERE a.employeeId = :employeeId AND YEAR(a.fecha) = :anio AND MONTH(a.fecha) = :mes")
    int totalMinutosExtra(@Param("employeeId") Long employeeId, @Param("mes") int mes, @Param("anio") int anio);

    // ── Dashboard ──────────────────────────────────────────────────

    @Query("SELECT COUNT(a) FROM AttendanceRecord a WHERE a.estado = 'AUSENCIA' AND a.fecha >= :desde AND a.fecha <= :hasta")
    long countAusenciasByPeriod(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("SELECT COUNT(DISTINCT a.employeeId) FROM AttendanceRecord a WHERE a.fecha >= :desde AND a.fecha <= :hasta")
    long countDistinctEmployeeWithAttendance(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("SELECT COALESCE(SUM(a.horasTrabajadasMinutos), 0) FROM AttendanceRecord a WHERE a.fecha >= :desde AND a.fecha <= :hasta")
    long totalMinutosTrabajadosByPeriod(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
