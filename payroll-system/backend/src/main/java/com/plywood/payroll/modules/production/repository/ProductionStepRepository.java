package com.plywood.payroll.modules.production.repository;

import com.plywood.payroll.modules.production.entity.ProductionStep;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionStepRepository extends JpaRepository<ProductionStep, Long> {
    java.util.Optional<ProductionStep> findByName(String name);
}
