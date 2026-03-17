package com.plywood.payroll.modules.pricing.dto.response;
import com.plywood.payroll.modules.quality.dto.response.ProductQualityResponse;
import com.plywood.payroll.modules.production.dto.response.ProductionStepResponse;
import com.plywood.payroll.modules.product.dto.response.ProductResponse;

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
