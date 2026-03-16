package com.plywood.payroll.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TeamRequest {
    @NotBlank(message = "Tên tổ không được để trống")
    private String name;

    @NotNull(message = "Công đoạn sản xuất không được để trống")
    private Long productionStepId;

    // Phòng ban phụ trách tổ này (optional, thêm mới)
    private Long departmentId;
}
