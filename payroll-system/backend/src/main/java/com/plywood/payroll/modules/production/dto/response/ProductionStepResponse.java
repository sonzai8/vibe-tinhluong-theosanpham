package com.plywood.payroll.modules.production.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProductionStepResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
