package com.plywood.payroll.modules.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyAttendanceResponse {
    private Long id;
    private LocalDate attendanceDate;
    
    private Long employeeId;
    private String employeeFullName;
    private String employeeCode;
    private Long employeeDepartmentId;
    private Long employeeTeamId;
    
    private Long originalTeamId;
    private String originalTeamName;
    
    private Long actualTeamId;
    private String actualTeamName;
    
    private Long attendanceDefinitionId;
    private String attendanceDefinitionCode;
    private String attendanceDefinitionName;
    private Double attendanceDefinitionMultiplier;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
