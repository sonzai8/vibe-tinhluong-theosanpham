package com.plywood.payroll.dto.request;

import jakarta.validation.constraints.DecimalMin;
import com.plywood.payroll.enums.FilmCoatingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {
    @NotBlank(message = "Mã sản phẩm không được để trống")
    private String code;

    @NotNull(message = "Độ dày không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Độ dày phải lớn hơn 0")
    private BigDecimal thickness;

    @NotNull(message = "Chiều dài không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Chiều dài phải lớn hơn 0")
    private BigDecimal length;

    @NotNull(message = "Chiều rộng không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Chiều rộng phải lớn hơn 0")
    private BigDecimal width;

    private FilmCoatingType filmCoatingType;
}
