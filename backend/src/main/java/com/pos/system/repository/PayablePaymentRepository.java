package com.pos.system.repository;

import com.pos.system.entity.PayablePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PayablePaymentRepository extends JpaRepository<PayablePayment, Long> {

    List<PayablePayment> findByPayableId(Long payableId);

    List<PayablePayment> findByFechaBetween(LocalDateTime desde, LocalDateTime hasta);
}
