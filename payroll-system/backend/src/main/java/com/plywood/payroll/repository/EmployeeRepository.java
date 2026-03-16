package com.plywood.payroll.repository;

import com.plywood.payroll.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUsername(String username);
    boolean existsByCode(String code);

    Optional<Employee> findByCode(String empCode);
}
