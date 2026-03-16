package com.plywood.payroll.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PenaltyBonusRequest {
    @NotNull(message = "Nhân viên không được để trống")
    private Long employeeId;

    @NotNull(message = "Ngày ghi nhận không được để trống")
    private LocalDate recordDate;

    @NotNull(message = "Số tiền không được để trống")
    private BigDecimal amount; // Dương = thưởng, Âm = phạt

    private String reason;
}
