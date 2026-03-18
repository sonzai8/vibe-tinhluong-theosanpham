package com.plywood.payroll.modules.payroll.repository;

import com.plywood.payroll.modules.payroll.entity.PayrollItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import java.util.Optional;

public interface PayrollItemRepository extends JpaRepository<PayrollItem, Long> {
    List<PayrollItem> findByPayrollId(Long payrollId);
    Optional<PayrollItem> findByPayrollIdAndEmployeeId(Long payrollId, Long employeeId);
    void deleteByPayrollId(Long payrollId);
}
