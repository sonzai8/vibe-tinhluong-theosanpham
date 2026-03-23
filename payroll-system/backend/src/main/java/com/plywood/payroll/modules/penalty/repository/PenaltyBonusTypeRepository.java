package com.plywood.payroll.modules.penalty.repository;

import com.plywood.payroll.modules.penalty.entity.PenaltyBonusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PenaltyBonusTypeRepository extends JpaRepository<PenaltyBonusType, Long> {
}
