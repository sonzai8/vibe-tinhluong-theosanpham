package com.plywood.payroll.modules.employee.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EmployeeAuditLogResponse {
    private Long id;
    private String action;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private String changedBy;
    private LocalDateTime changedAt;
}
