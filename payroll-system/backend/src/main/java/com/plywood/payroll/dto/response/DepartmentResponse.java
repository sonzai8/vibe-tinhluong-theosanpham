package com.plywood.payroll.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DepartmentResponse {
    private Long id;
    private String name;
    private Integer teamCount;
    private java.util.List<String> teamNames;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
