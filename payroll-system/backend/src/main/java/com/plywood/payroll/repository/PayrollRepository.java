package com.plywood.payroll.repository;

import com.plywood.payroll.entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PayrollRepository extends JpaRepository<Payroll, Long> {
    Optional<Payroll> findByMonthAndYear(int month, int year);
}
