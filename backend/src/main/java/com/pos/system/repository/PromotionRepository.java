package com.pos.system.repository;

import com.pos.system.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    @Query("SELECT p FROM Promotion p WHERE p.activa = true " +
           "AND p.fechaDesde <= :hoy AND p.fechaHasta >= :hoy " +
           "ORDER BY p.prioridad DESC")
    List<Promotion> findActivePromotions(@Param("hoy") LocalDate hoy);

    @Query("SELECT p FROM Promotion p WHERE p.activa = true " +
           "AND p.alcance = 'PRODUCTO' AND p.fechaDesde <= :hoy AND p.fechaHasta >= :hoy")
    List<Promotion> findActiveProductPromotions(@Param("hoy") LocalDate hoy);

    @Query("SELECT p FROM Promotion p WHERE p.activa = true " +
           "AND p.alcance = 'CATEGORIA' AND p.fechaDesde <= :hoy AND p.fechaHasta >= :hoy")
    List<Promotion> findActiveCategoryPromotions(@Param("hoy") LocalDate hoy);

    @Query("SELECT p FROM Promotion p WHERE p.activa = true " +
           "AND p.alcance = 'CARRITO' AND p.fechaDesde <= :hoy AND p.fechaHasta >= :hoy")
    List<Promotion> findActiveCartPromotions(@Param("hoy") LocalDate hoy);
}
