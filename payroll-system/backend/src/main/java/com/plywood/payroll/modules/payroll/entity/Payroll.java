package com.plywood.payroll.modules.payroll.entity;
import com.plywood.payroll.shared.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "payrolls", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"month", "year"})
})
public class Payroll extends BaseEntity {
    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private String status = "DRAFT"; // DRAFT, CONFIRMED
}
