package com.plywood.payroll.modules.employee.dto.response;

import com.plywood.payroll.modules.organization.dto.response.TeamResponse;
import lombok.Data;
import java.time.LocalDate;

@Data
public class TeamProcessResponse {
    private Long id;
    private TeamResponse team;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isCurrent;
}
