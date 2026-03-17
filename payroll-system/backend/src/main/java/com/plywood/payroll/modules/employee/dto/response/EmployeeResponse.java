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
    private String fullName;
    private TeamResponse team;
    private DepartmentResponse department;
    private RoleResponse role;
    private String status;
    private String username;
    private String phone;
    private String citizenId;
    private boolean canLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
