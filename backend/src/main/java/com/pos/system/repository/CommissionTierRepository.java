package com.pos.system.repository;

import com.pos.system.entity.CommissionTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommissionTierRepository extends JpaRepository<CommissionTier, Long> {

    List<CommissionTier> findBySchemeIdOrderByMontoDesdeAsc(Long schemeId);
}
