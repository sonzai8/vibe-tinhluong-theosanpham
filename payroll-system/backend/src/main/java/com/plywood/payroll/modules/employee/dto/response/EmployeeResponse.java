package com.plywood.payroll.modules.employee.dto.response;
import com.plywood.payroll.modules.organization.dto.response.RoleResponse;
import com.plywood.payroll.modules.organization.dto.response.DepartmentResponse;
import com.plywood.payroll.modules.organization.dto.response.TeamResponse;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmployeeResponse {
    private Long id;
    private String code;
    private String zkDeviceId;
    private String fullName;
    private TeamResponse team;
    private DepartmentResponse department;
    private RoleResponse role;
    private String status;
    private String username;
    private String phone;
    private String citizenId;
    private boolean canLogin;

    private String gender;
    private java.time.LocalDate dob;
    private java.time.LocalDate joinDate;
    private java.time.LocalDate insuranceStartDate;
    private java.time.LocalDate citizenIdIssuedDate;
    private java.time.LocalDate lastWorkingDate;
    private String citizenIdIssuedPlace;
    private String birthAddress;
    private String permanentAddress;
    private String notes;
    private String avatarUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private com.plywood.payroll.modules.employee.entity.SalaryType salaryType;
    private java.math.BigDecimal baseSalaryConfig;
    private java.math.BigDecimal insuranceSalaryConfig;
}
