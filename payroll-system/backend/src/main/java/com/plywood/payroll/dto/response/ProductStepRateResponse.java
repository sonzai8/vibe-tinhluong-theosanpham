package com.plywood.payroll.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ProductStepRateResponse {
    private Long id;
    private ProductResponse product;
    private ProductionStepResponse productionStep;
    private BigDecimal basePrice;
    private LocalDate effectiveDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
