package com.pos.system.repository.refund;

import com.pos.system.entity.refund.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long>, JpaSpecificationExecutor<Refund> {

    Optional<Refund> findByReferenceNumber(String referenceNumber);

    List<Refund> findByBusinessId(Long businessId);

    List<Refund> findByStatus(String status);

    List<Refund> findByPaymentId(Long paymentId);

    List<Refund> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    boolean existsByReferenceNumber(String referenceNumber);
}
