package com.plywood.payroll.modules.attendance.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class AttendanceDefinitionResponse {
    private Long id;
    private String code;
    private String name;
    private Double multiplier;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
