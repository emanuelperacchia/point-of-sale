package com.pos.system.repository;

import com.pos.system.entity.Sale;
import com.pos.system.entity.SaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    Page<Sale> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Sale> findByClientIdOrderByCreatedAtDesc(Long clientId, Pageable pageable);

    Page<Sale> findByStatusOrderByCreatedAtDesc(SaleStatus status, Pageable pageable);

    long countByStatus(SaleStatus status);
}
