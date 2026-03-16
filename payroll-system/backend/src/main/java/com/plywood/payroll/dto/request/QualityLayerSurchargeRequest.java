package com.plywood.payroll.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class QualityLayerSurchargeRequest {
    @NotBlank(message = "Tên loại lớp lỗi không được để trống")
    private String layerType;

    @NotNull(message = "Mức phạt lớp lỗi không được để trống")
    private BigDecimal surchargePerLayer;
}
