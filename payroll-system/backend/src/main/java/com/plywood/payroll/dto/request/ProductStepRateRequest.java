package com.plywood.payroll.dto.request;

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

    @NotNull(message = "Đơn giá không được để trống")
    private BigDecimal basePrice;

    @NotNull(message = "Ngày hiệu lực không được để trống")
    private LocalDate effectiveDate;
}
