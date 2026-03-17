package com.plywood.payroll.modules.product.repository;

import com.plywood.payroll.modules.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
