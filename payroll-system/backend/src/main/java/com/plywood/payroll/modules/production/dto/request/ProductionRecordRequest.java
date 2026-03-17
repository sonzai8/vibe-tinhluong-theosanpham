package com.plywood.payroll.modules.production.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ProductionRecordRequest {
    @NotNull(message = "Ngày sản xuất không được để trống")
    private LocalDate productionDate;

    @NotNull(message = "Tổ sản xuất không được để trống")
    private Long teamId;

    @NotNull(message = "Sản phẩm không được để trống")
    private Long productId;

    @NotNull(message = "Chất lượng không được để trống")
    private Long qualityId;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;
}
