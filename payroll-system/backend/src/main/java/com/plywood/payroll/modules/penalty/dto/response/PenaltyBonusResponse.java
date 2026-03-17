package com.plywood.payroll.modules.penalty.dto.response;
import com.plywood.payroll.modules.employee.dto.response.EmployeeResponse;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PenaltyBonusResponse {
    private Long id;
    private EmployeeResponse employee;
    private LocalDate recordDate;
    private BigDecimal amount;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
