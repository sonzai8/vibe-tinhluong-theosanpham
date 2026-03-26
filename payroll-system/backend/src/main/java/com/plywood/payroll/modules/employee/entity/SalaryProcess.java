package com.plywood.payroll.modules.employee.entity;

import com.plywood.payroll.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "salary_process")
public class SalaryProcess extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SalaryType salaryType;

    @Column(nullable = false)
    private BigDecimal baseSalary = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal insuranceSalary = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Column(length = 255)
    private String note;
}
