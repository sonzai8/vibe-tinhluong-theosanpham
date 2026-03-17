package com.plywood.payroll.modules.attendance.entity;

import com.plywood.payroll.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "attendance_definitions")
public class AttendanceDefinition extends BaseEntity {

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Double multiplier = 1.0;

    @Column(length = 255)
    private String description;
}
