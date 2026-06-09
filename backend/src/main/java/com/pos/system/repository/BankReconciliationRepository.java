package com.pos.system.repository;

import com.pos.system.entity.BankReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankReconciliationRepository extends JpaRepository<BankReconciliation, Long> {

    Optional<BankReconciliation> findByPeriodo(String periodo);
}
