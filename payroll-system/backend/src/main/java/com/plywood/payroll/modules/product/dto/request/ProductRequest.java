package com.plywood.payroll.modules.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import com.plywood.payroll.modules.product.enums.FilmCoatingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {
    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    @NotBlank(message = "Mã sản phẩm không được để trống")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Mã sản phẩm chỉ được chứa chữ cái IN HOA, số và dấu gạch dưới")
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

    @NotNull(message = "Đơn vị sản phẩm không được để trống")
    private Long unitId;
}
