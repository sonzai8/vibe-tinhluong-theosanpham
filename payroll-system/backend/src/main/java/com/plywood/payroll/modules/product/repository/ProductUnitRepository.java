package com.plywood.payroll.modules.product.repository;

import com.plywood.payroll.modules.product.entity.ProductUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductUnitRepository extends JpaRepository<ProductUnit, Long> {
    Optional<ProductUnit> findByName(String name);
}
