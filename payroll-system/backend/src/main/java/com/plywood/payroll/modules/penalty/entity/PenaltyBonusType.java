package com.plywood.payroll.modules.penalty.entity;

import com.plywood.payroll.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
@Entity
@Table(name = "penalty_bonus_types")
public class PenaltyBonusType extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private BigDecimal defaultAmount;

    private String description;
}
