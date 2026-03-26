package com.plywood.payroll.modules.employee.repository;

import com.plywood.payroll.modules.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUsername(String username);
    Optional<Employee> findByZkDeviceId(String zkDeviceId);
    boolean existsByCode(String code);
    boolean existsByCitizenId(String citizenId);
    boolean existsByPhone(String phone);

    Optional<Employee> findByCode(String empCode);
    
    @org.springframework.data.jpa.repository.Query("SELECT e FROM Employee e JOIN TeamProcess tp ON tp.employee = e WHERE tp.team.id = :teamId " +
            "AND tp.startDate <= CURRENT_DATE AND (tp.endDate IS NULL OR tp.endDate >= CURRENT_DATE)")
    java.util.List<Employee> findAllByTeamId(@org.springframework.data.repository.query.Param("teamId") Long teamId);
    
    long countByStatus(String status);
    
    long countByDepartment_IdAndStatus(Long departmentId, String status);

    @org.springframework.data.jpa.repository.Query("SELECT e.code FROM Employee e WHERE e.code LIKE :prefix%")
    java.util.List<String> findCodesByPrefix(@org.springframework.data.repository.query.Param("prefix") String prefix);

    @org.springframework.data.jpa.repository.Query("SELECT e FROM Employee e WHERE e.status = 'ACTIVE' AND (LOWER(e.fullName) LIKE LOWER(:search) OR LOWER(e.code) LIKE LOWER(:search))")
    java.util.List<Employee> search(@org.springframework.data.repository.query.Param("search") String search);
}
