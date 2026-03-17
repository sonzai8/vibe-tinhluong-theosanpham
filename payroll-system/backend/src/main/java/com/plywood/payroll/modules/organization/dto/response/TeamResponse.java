package com.plywood.payroll.modules.organization.dto.response;
import com.plywood.payroll.modules.production.dto.response.ProductionStepResponse;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TeamResponse {
    private Long id;
    private String name;
    private DepartmentResponse department;
    private ProductionStepResponse productionStep;
    private Integer memberCount;
    private java.util.List<String> memberNames;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
