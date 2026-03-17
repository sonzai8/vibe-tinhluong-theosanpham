package com.plywood.payroll.modules.organization.entity;
import com.plywood.payroll.shared.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "departments")
public class Department extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String name;
}
