package com.plywood.payroll.modules.organization.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TeamMemberRequest {
    @NotNull(message = "Nhân viên không được để trống")
    private Long employeeId;

    @NotNull(message = "Ngày tham gia không được để trống")
    private LocalDate joinedDate;
}
