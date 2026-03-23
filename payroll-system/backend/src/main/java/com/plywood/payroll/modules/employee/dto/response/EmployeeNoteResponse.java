package com.plywood.payroll.modules.employee.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EmployeeNoteResponse {
    private Long id;
    private String content;
    private Integer month;
    private Integer year;
    private String createdBy;
    private LocalDateTime createdAt;
}
