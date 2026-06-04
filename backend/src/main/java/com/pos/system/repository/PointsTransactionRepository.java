package com.pos.system.repository;

import com.pos.system.entity.PointsTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PointsTransactionRepository extends JpaRepository<PointsTransaction, Long> {
    Page<PointsTransaction> findByClientIdOrderByFechaDesc(Long clientId, Pageable pageable);

    List<PointsTransaction> findByClientIdAndFechaBefore(Long clientId, LocalDateTime before);
}
