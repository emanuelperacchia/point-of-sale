package com.pos.system.repository;

import com.pos.system.entity.ReceivablePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReceivablePaymentRepository extends JpaRepository<ReceivablePayment, Long> {

    List<ReceivablePayment> findByReceivableId(Long receivableId);

    List<ReceivablePayment> findByFechaBetween(LocalDateTime desde, LocalDateTime hasta);
}
