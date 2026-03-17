package com.plywood.payroll.modules.organization.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class RoleResponse {
    private Long id;
    private String name;
    private BigDecimal dailyBenefit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<String> permissions;
}
