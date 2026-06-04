package com.pos.system.repository;

import com.pos.system.entity.Payment;
import com.pos.system.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findBySale(Sale sale);

    List<Payment> findBySaleId(Long saleId);

    List<Payment> findByShiftId(Long shiftId);
}
