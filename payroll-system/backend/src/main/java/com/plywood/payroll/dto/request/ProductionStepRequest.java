package com.plywood.payroll.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductionStepRequest {
    @NotBlank(message = "Tên công đoạn không được để trống")
    private String name;

    private String description;
}
