package com.plywood.payroll.modules.quality.repository;

import com.plywood.payroll.modules.quality.entity.ProductQuality;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductQualityRepository extends JpaRepository<ProductQuality, Long> {
}
