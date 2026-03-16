package com.plywood.payroll.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RoleResponse {
    private Long id;
    private String name;
    private BigDecimal dailyBenefit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
