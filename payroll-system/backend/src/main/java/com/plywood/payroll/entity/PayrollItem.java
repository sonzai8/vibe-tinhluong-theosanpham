package com.plywood.payroll.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
@Entity
@Table(name = "payroll_items", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"payroll_id", "employee_id"})
})
public class PayrollItem extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "payroll_id")
    private Payroll payroll;

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    private BigDecimal totalStepSalary = BigDecimal.ZERO; // Lương chia đều từ tổ
    private BigDecimal totalBenefit = BigDecimal.ZERO;    // Phụ cấp chức vụ theo ngày
    private BigDecimal totalBonus = BigDecimal.ZERO;      // Thưởng
    private BigDecimal totalPenalty = BigDecimal.ZERO;    // Phạt
    private BigDecimal netSalary = BigDecimal.ZERO;       // Thực nhận
}
