package com.pos.system.repository;

import com.pos.system.entity.BankStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankStatementRepository extends JpaRepository<BankStatement, Long> {

    List<BankStatement> findByReconciliationId(Long reconciliationId);

    List<BankStatement> findByReconciliationIdAndEstado(Long reconciliationId, BankStatement.EstadoConciliacion estado);

    List<BankStatement> findByReconciliationIdAndEstadoIn(
            Long reconciliationId, List<BankStatement.EstadoConciliacion> estados);

    long countByReconciliationIdAndEstado(Long reconciliationId, BankStatement.EstadoConciliacion estado);
}
