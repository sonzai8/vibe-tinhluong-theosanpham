package com.plywood.payroll.modules.organization.entity;

import com.plywood.payroll.modules.production.entity.ProductionStep;
import com.plywood.payroll.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@Entity
@Table(name = "teams")
public class Team extends BaseEntity {
    @Column(nullable = false)
    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "production_step_id")
    private ProductionStep productionStep;

    // Phòng ban quản lý tổ này
    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

}
