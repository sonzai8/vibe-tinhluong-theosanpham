package com.plywood.payroll.modules.employee.entity;

import com.plywood.payroll.modules.organization.entity.Department;
import com.plywood.payroll.modules.organization.entity.Role;
import com.plywood.payroll.modules.organization.entity.Team;
import com.plywood.payroll.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "employees")
public class Employee extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String fullName;

    // Tổ biên chế trực tiếp của nhân viên
    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    // Giữ lại để backward compat (employee.department có thể suy ra từ team.department)
    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    // Chức vụ (alias là Position, bao gồm phụ cấp ngày)
    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    @Column(nullable = false)
    private String status = "ACTIVE";

    // Tài khoản đăng nhập (nếu người dùng hệ thống)
    private String username;
    private String password;

    private boolean canLogin = false;

    @Column(length = 20)
    private String phone;

    @Column(length = 20)
    private String citizenId;
}
