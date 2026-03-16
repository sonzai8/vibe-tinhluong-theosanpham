package com.plywood.payroll.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class RoleRequest {
    @NotBlank(message = "Tên chức vụ không được để trống")
    private String name;

    @NotNull(message = "Phụ cấp chức vụ không được để trống")
    private BigDecimal dailyBenefit;

    private java.util.Set<String> permissions;
}
