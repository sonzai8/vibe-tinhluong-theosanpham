package com.plywood.payroll.modules.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductUnitRequest {
    @NotBlank(message = "Tên đơn vị không được để trống")
    private String name;
}
