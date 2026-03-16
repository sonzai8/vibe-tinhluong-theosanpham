package com.plywood.payroll.repository;

import com.plywood.payroll.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
