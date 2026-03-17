package com.plywood.payroll.modules.quality.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QualityLayerSurchargeResponse {
    private Long id;
    private String layerType;
    private BigDecimal surchargePerLayer;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
