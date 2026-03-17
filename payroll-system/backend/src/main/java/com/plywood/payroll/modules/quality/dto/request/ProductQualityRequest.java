package com.plywood.payroll.modules.quality.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class ProductQualityRequest {
    @NotBlank(message = "Mã loại chất lượng không được để trống")
    private String code;

    private String description;
    
    private List<QualityLayerRequest> layers;
    
    @Data
    public static class QualityLayerRequest {
        @NotNull(message = "Lớp lỗi không được để trống")
        private Long layerId;
        
        @NotNull(message = "Số lượng không được để trống")
        private Integer quantity;
    }
}
