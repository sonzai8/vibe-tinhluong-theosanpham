package com.plywood.payroll.repository;

import com.plywood.payroll.entity.ProductionStep;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionStepRepository extends JpaRepository<ProductionStep, Long> {
}
