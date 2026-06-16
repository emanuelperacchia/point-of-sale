package com.pos.system.repository;

import com.pos.system.entity.LoteProduccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoteProduccionRepository extends JpaRepository<LoteProduccion, Long> {
    Optional<LoteProduccion> findByNumeroLote(String numeroLote);
    LoteProduccion findByProductionOrderId(Long productionOrderId);
}
