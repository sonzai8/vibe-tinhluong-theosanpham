package com.plywood.payroll.modules.production.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionRecordResponse {
    private Long id;
    private LocalDate productionDate;
    
    private Long teamId;
    private String teamName;
    private Long teamProductionStepId;
    private String teamStepName;
    
    private Long productId;
    private String productCode;
    private String productName;
    private java.math.BigDecimal productThickness;
    private java.math.BigDecimal productLength;
    private java.math.BigDecimal productWidth;
    
    private Long qualityId;
    private String qualityCode;
    
    private Integer quantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
