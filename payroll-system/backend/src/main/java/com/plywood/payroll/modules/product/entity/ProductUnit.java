package com.plywood.payroll.modules.product.entity;

import com.plywood.payroll.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "product_units")
public class ProductUnit extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String name; // Ví dụ: TẤM, KHÚC, MIẾNG, THÙNG, CÔNG NHẬT
}
