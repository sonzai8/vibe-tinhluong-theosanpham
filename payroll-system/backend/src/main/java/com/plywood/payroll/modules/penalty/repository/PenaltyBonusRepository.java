package com.plywood.payroll.modules.penalty.repository;

import com.plywood.payroll.modules.penalty.entity.PenaltyBonus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PenaltyBonusRepository extends JpaRepository<PenaltyBonus, Long> {
    List<PenaltyBonus> findByEmployeeId(Long employeeId);
    
    // Tìm khen thưởng kỷ luật trong tháng/năm
    List<PenaltyBonus> findByEmployeeIdAndRecordDateBetween(Long employeeId, java.time.LocalDate startDate, java.time.LocalDate endDate);

    @org.springframework.data.jpa.repository.Query("SELECT pb FROM PenaltyBonus pb WHERE pb.recordDate BETWEEN :startDate AND :endDate " +
            "AND (:departmentId IS NULL OR pb.employee.department.id = :departmentId) " +
            "AND (:teamId IS NULL OR pb.employee.team.id = :teamId)")
    List<PenaltyBonus> findByFilters(java.time.LocalDate startDate, java.time.LocalDate endDate, Long departmentId, Long teamId);
}
