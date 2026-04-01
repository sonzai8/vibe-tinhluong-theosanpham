package com.plywood.payroll.modules.production.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductionStepRequest {
    @NotBlank(message = "Tên công đoạn không được để trống")
    private String name;

    private String description;

    private com.plywood.payroll.modules.production.enums.PriceCalculationType priceCalculationType;

    private java.util.List<Long> productIds;
}
