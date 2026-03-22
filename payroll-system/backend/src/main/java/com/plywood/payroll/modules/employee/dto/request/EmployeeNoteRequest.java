package com.plywood.payroll.modules.employee.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmployeeNoteRequest {
    @NotBlank(message = "Nội dung ghi chú không được để trống")
    private String content;
    
    private Integer month;
    private Integer year;
}
