package com.plywood.payroll.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "production_steps")
public class ProductionStep extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String name;

    private String description;
}
