package com.pos.system.repository;

import com.pos.system.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    Page<Alert> findByResolvedFalseOrderByCreatedAtDesc(Pageable pageable);

    List<Alert> findByResolvedFalseAndReadFalseOrderByCreatedAtDesc();

    long countByResolvedFalse();

    long countByResolvedFalseAndReadFalse();

    @Query("SELECT a FROM Alert a WHERE a.product.id = :productId AND a.resolved = false")
    List<Alert> findActiveAlertsByProduct(@Param("productId") Long productId);

    @Query("SELECT a FROM Alert a WHERE a.createdAt BETWEEN :start AND :end AND a.type = :type")
    List<Alert> findAlertsByDateRangeAndType(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("type") Alert.AlertType type);

    List<Alert> findByResolvedFalseAndCreatedAtBefore(LocalDateTime dateTime);
}