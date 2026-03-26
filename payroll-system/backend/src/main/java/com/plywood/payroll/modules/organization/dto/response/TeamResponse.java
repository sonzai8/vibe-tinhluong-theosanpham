package com.plywood.payroll.modules.organization.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamResponse {
    private Long id;
    private String name;
    private Long departmentId;
    private String departmentName;
    private Long productionStepId;
    private String productionStepName;
    private Integer memberCount;
    private List<String> memberNames;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
