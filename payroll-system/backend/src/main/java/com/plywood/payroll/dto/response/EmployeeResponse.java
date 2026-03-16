package com.plywood.payroll.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmployeeResponse {
    private Long id;
    private String code;
    private String fullName;
    private DepartmentResponse department;
    private RoleResponse role;
    private String status;
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
