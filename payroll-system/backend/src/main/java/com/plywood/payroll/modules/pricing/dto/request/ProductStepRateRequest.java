package com.plywood.payroll.modules.pricing.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductStepRateRequest {
    @NotNull(message = "Sản phẩm không được để trống")
    private Long productId;

    @NotNull(message = "Công đoạn không được để trống")
    private Long productionStepId;

    @NotNull(message = "Chất lượng không được để trống")
    private Long qualityId;

    @NotNull(message = "Đơn giá chuyên cần không được để trống")
    private BigDecimal priceHigh;

    @NotNull(message = "Đơn giá thường không được để trống")
    private BigDecimal priceLow;

    @NotNull(message = "Ngày hiệu lực không được để trống")
    private LocalDate effectiveDate;
}
