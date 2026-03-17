package com.plywood.payroll.modules.organization.entity;

import com.plywood.payroll.modules.employee.entity.Employee;
import com.plywood.payroll.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
@Entity
@Table(name = "team_members")
public class TeamMember extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "team_id")
    private Team team;

    @OneToOne(optional = false)
    @JoinColumn(name = "employee_id", unique = true)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate joinedDate;
}
