package com.plywood.payroll.modules.employee.entity;

import com.plywood.payroll.modules.organization.entity.Department;
import com.plywood.payroll.modules.organization.entity.Role;
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

    @Column(unique = true)
    private String zkDeviceId; // ID trên máy chấm công ZKTeco

    @Column(nullable = false)
    private String fullName;

    // Department represented in Employee as core affiliation
    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    private String bankAccountNumber;

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

    private String gender;
    private java.time.LocalDate dob;
    private java.time.LocalDate joinDate;
    private java.time.LocalDate insuranceStartDate;
    private java.time.LocalDate citizenIdIssuedDate;
    private java.time.LocalDate lastWorkingDate;
    private String citizenIdIssuedPlace;
    private String birthAddress;
    private String permanentAddress;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    private String avatarPath;
}
