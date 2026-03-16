package com.plywood.payroll.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
@Entity
@Table(name = "penalties_bonuses")
public class PenaltyBonus extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(nullable = false)
    private LocalDate recordDate;

    @Column(nullable = false)
    private BigDecimal amount; // Dương = thưởng, Âm = phạt

    private String reason;
}
