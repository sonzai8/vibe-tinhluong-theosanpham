package com.plywood.payroll.repository;

import com.plywood.payroll.entity.PenaltyBonus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PenaltyBonusRepository extends JpaRepository<PenaltyBonus, Long> {
    List<PenaltyBonus> findByEmployeeId(Long employeeId);
    
    // Tìm khen thưởng kỷ luật trong tháng/năm
    List<PenaltyBonus> findByEmployeeIdAndRecordDateBetween(Long employeeId, java.time.LocalDate startDate, java.time.LocalDate endDate);
}
