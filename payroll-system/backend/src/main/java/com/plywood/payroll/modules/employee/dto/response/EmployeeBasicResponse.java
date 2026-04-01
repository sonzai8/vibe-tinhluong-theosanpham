package com.plywood.payroll.modules.employee.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmployeeBasicResponse {
    private Long id;
    private String code;
    private String fullName;
    private String teamName;
}
