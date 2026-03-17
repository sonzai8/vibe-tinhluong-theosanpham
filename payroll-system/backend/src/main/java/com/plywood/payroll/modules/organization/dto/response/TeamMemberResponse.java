package com.plywood.payroll.modules.organization.dto.response;
import com.plywood.payroll.modules.employee.dto.response.EmployeeResponse;

import lombok.Data;
import java.time.LocalDate;

@Data
public class TeamMemberResponse {
    private Long id;
    private EmployeeResponse employee;
    private LocalDate joinedDate;
}
