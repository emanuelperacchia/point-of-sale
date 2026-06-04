package com.pos.system.repository;

import com.pos.system.entity.ShiftMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShiftMovementRepository extends JpaRepository<ShiftMovement, Long> {
    List<ShiftMovement> findByShiftIdOrderByCreatedAtAsc(Long shiftId);
}
