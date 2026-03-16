package com.plywood.payroll.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStepRateResponse {
    private Long id;
    private ProductResponse product;
    private ProductionStepResponse productionStep;
    private ProductQualityResponse quality;
    private BigDecimal priceHigh;
    private BigDecimal priceLow;
    private LocalDate effectiveDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
