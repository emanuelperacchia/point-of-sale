package com.pos.system.repository;

import com.pos.system.entity.EmployeeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeHistoryRepository extends JpaRepository<EmployeeHistory, Long> {

    List<EmployeeHistory> findByEmployeeIdOrderByFechaDesc(Long employeeId);
}
