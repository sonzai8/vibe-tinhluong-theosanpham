package com.plywood.payroll.modules.payroll.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PayrollResponse {
    private Long id;
    private Integer month;
    private Integer year;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
