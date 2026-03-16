package com.plywood.payroll.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductResponse {
    private Long id;
    private String code;
    private BigDecimal thickness;
    private BigDecimal length;
    private BigDecimal width;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
