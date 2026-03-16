package com.plywood.payroll.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class DailyAttendanceRequest {
    @NotNull(message = "Ngày điểm danh không được để trống")
    private LocalDate attendanceDate;

    @NotNull(message = "Nhân viên không được để trống")
    private Long employeeId;

    private Long originalTeamId; // Tổ biên chế

    private Long actualTeamId; // Tổ thực tế làm việc
}
