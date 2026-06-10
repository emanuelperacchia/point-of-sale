package com.pos.system.repository;

import com.pos.system.entity.ShiftChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShiftChangeRequestRepository extends JpaRepository<ShiftChangeRequest, Long> {

    List<ShiftChangeRequest> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<ShiftChangeRequest> findByEstadoOrderByCreatedAtAsc(ShiftChangeRequest.Estado estado);
}
