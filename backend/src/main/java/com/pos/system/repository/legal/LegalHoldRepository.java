package com.pos.system.repository.legal;

import com.pos.system.entity.legal.LegalHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LegalHoldRepository extends JpaRepository<LegalHold, Long>, JpaSpecificationExecutor<LegalHold> {

    List<LegalHold> findByBusinessId(Long businessId);

    List<LegalHold> findByActiveTrue();

    List<LegalHold> findByEffectiveFromBeforeAndEffectiveUntilAfter(LocalDateTime from, LocalDateTime until);

    List<LegalHold> findByDataDeletionDateBefore(LocalDateTime date);

    boolean existsByBusinessIdAndActiveTrue(Long businessId);
}
