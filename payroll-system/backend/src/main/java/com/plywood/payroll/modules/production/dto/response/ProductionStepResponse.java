package com.plywood.payroll.modules.production.dto.response;

import lombok.Data;

@Data
public class ProductionStepResponse {
    private Long id;
    private String name;
    private String description;
    private com.plywood.payroll.modules.production.enums.PriceCalculationType priceCalculationType;
    private java.util.List<com.plywood.payroll.modules.product.dto.response.ProductResponse> products;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}
