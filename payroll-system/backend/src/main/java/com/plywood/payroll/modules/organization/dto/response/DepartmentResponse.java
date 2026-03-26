package com.plywood.payroll.modules.organization.dto.response;
import com.plywood.payroll.modules.organization.dto.response.TeamResponse;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DepartmentResponse {
    private Long id;
    private String name;
    private Integer teamCount;
    private java.util.List<TeamResponse> teams;
    private java.util.List<String> teamNames;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
