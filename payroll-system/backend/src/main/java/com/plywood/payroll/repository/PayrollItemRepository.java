package com.plywood.payroll.repository;

import com.plywood.payroll.entity.PayrollItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PayrollItemRepository extends JpaRepository<PayrollItem, Long> {
    List<PayrollItem> findByPayrollId(Long payrollId);
    void deleteByPayrollId(Long payrollId);
}
