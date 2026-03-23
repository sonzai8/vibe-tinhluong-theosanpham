package com.plywood.payroll.modules.quality.dto.response;
import com.plywood.payroll.modules.quality.entity.ProductQualityLayer;

import lombok.Data;
import java.util.List;
import java.time.LocalDateTime;

@Data
public class ProductQualityResponse {
    private Long id;
    private String code;
    private String description;
    private Integer priority;
    private List<QualityLayerResponse> layers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    public static class QualityLayerResponse {
        private Long id; // ID của ProductQualityLayer
        private QualityLayerSurchargeResponse layer; // Thông tin lớp lỗi
        private Integer quantity; // Số lượng
    }
}
