package com.plywood.payroll.dto.response;

import lombok.Data;
import java.time.LocalDate;

@Data
public class TeamMemberResponse {
    private Long id;
    private EmployeeResponse employee;
    private LocalDate joinedDate;
}
