package com.pos.system.repository.payment;

import com.pos.system.entity.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByBusinessId(Long businessId);

    List<Payment> findByStatus(String status);

    List<Payment> findByBusinessIdAndStatus(Long businessId, String status);

    List<Payment> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    BigDecimal sumByBusinessIdAndStatus(Long businessId, String status);
}
