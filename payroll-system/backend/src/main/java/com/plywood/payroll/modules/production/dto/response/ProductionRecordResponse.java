package com.plywood.payroll.modules.production.dto.response;
import com.plywood.payroll.modules.organization.dto.response.TeamResponse;
import com.plywood.payroll.modules.quality.dto.response.ProductQualityResponse;
import com.plywood.payroll.modules.product.dto.response.ProductResponse;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ProductionRecordResponse {
    private Long id;
    private LocalDate productionDate;
    private TeamResponse team;
    private ProductResponse product;
    private ProductQualityResponse quality;
    private Integer quantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
