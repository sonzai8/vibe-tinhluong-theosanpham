package com.plywood.payroll.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "payroll_configs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"configKey", "configMonth", "configYear"})
})
public class PayrollConfig extends BaseEntity {
    @Column(nullable = false)
    private String configKey; // e.g., "MIN_ATTENDANCE_DAYS"

    @Column(name = "config_month")
    private Integer month;

    @Column(name = "config_year")
    private Integer year;

    @Column(nullable = false)
    private String configValue;

    private String description;
}
