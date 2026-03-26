package com.plywood.payroll.modules.employee.repository;

import com.plywood.payroll.modules.employee.entity.SalaryProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SalaryProcessRepository extends JpaRepository<SalaryProcess, Long>, JpaSpecificationExecutor<SalaryProcess> {

    @Query("SELECT s FROM SalaryProcess s WHERE s.employee.id = :employeeId " +
           "AND s.startDate <= :date AND (s.endDate IS NULL OR s.endDate >= :date)")
    Optional<SalaryProcess> findEffectiveByDate(@Param("employeeId") Long employeeId, @Param("date") LocalDate date);

    @Query("SELECT s FROM SalaryProcess s WHERE s.employee.id = :employeeId " +
           "AND s.startDate <= :endDate AND (s.endDate IS NULL OR s.endDate >= :startDate)")
    List<SalaryProcess> findOverlapping(@Param("employeeId") Long employeeId, 
                                       @Param("startDate") LocalDate startDate, 
                                       @Param("endDate") LocalDate endDate);

    List<SalaryProcess> findByEmployeeIdOrderByStartDateDesc(Long employeeId);
}
