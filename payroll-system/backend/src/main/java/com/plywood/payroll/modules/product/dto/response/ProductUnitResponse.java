package com.plywood.payroll.modules.product.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProductUnitResponse {
    private Long id;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
