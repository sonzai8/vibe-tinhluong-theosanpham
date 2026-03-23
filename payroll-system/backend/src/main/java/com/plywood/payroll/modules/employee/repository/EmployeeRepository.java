package com.plywood.payroll.modules.employee.repository;

import com.plywood.payroll.modules.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUsername(String username);
    boolean existsByCode(String code);

    Optional<Employee> findByCode(String empCode);
    
    java.util.List<Employee> findAllByTeamId(Long teamId);
    
    long countByStatus(String status);
    
    long countByDepartment_IdAndStatus(Long departmentId, String status);

    @org.springframework.data.jpa.repository.Query("SELECT e.code FROM Employee e WHERE e.code LIKE :prefix%")
    java.util.List<String> findCodesByPrefix(@org.springframework.data.repository.query.Param("prefix") String prefix);
}
