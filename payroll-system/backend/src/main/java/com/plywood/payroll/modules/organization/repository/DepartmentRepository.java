package com.plywood.payroll.modules.organization.repository;

import com.plywood.payroll.modules.organization.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByName(String name);
}
