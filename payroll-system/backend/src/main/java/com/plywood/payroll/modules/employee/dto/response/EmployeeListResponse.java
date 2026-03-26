package com.plywood.payroll.modules.employee.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class EmployeeListResponse {
    private Long id;
    private String code;
    private String zkDeviceId;
    private String fullName;
    private String phone;
    private String citizenId;
    private String avatarUrl;
    private String status;
    private boolean canLogin;
    
    // Flattened Dept / Team / Role
    private Long departmentId;
    private String departmentName;
    private Long teamId;
    private String teamName;
    private Long roleId;
    private String roleName;
    
    // Quick Configs
    private String salaryType;
    private BigDecimal baseSalaryConfig;
    private BigDecimal insuranceSalaryConfig;
}
