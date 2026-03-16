package com.plywood.payroll.dto.response;

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
