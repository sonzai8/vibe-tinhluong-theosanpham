package com.plywood.payroll.modules.product.dto.response;

import com.plywood.payroll.modules.product.enums.FilmCoatingType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private String code;
    private BigDecimal thickness;
    private BigDecimal length;
    private BigDecimal width;
    private FilmCoatingType filmCoatingType;
    private ProductUnitResponse unit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
