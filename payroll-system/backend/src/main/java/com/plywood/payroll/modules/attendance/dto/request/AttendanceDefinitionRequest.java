package com.plywood.payroll.modules.attendance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttendanceDefinitionRequest {
    @NotBlank(message = "Mã loại công không được để trống")
    private String code;

    @NotBlank(message = "Tên loại công không được để trống")
    private String name;

    @NotNull(message = "Hệ số công không được để trống")
    private Double multiplier;

    private String description;
}
