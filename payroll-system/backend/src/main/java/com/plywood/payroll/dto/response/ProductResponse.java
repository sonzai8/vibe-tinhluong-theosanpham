package com.plywood.payroll.dto.response;

import com.plywood.payroll.enums.FilmCoatingType;
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
    private FilmCoatingType filmCoatingType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
