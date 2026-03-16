package com.plywood.payroll.dto.response;

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
