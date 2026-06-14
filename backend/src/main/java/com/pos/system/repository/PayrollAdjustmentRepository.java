package com.pos.system.repository;

import com.pos.system.entity.PayrollAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayrollAdjustmentRepository extends JpaRepository<PayrollAdjustment, Long> {
    List<PayrollAdjustment> findByPayrollId(Long payrollId);
}
