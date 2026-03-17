package com.plywood.payroll.modules.attendance.dto.response;
import com.plywood.payroll.modules.employee.dto.response.EmployeeResponse;
import com.plywood.payroll.modules.organization.dto.response.TeamResponse;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DailyAttendanceResponse {
    private Long id;
    private LocalDate attendanceDate;
    private EmployeeResponse employee;
    private TeamResponse originalTeam;
    private TeamResponse actualTeam;
    private AttendanceDefinitionResponse attendanceDefinition;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
