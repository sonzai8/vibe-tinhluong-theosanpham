package com.plywood.payroll.modules.employee.dto.request;

import lombok.Data;
import java.time.LocalDate;

/**
 * DTO để cập nhật thông tin một bản ghi lịch sử công tác (TeamProcess).
 * Cho phép sửa ngày bắt đầu, ngày kết thúc và ghi chú.
 */
@Data
public class TeamHistoryUpdateRequest {
    /** Ngày bắt đầu mới của bản ghi */
    private LocalDate startDate;

    /** Ngày kết thúc mới (null = đang là tổ hiện tại) */
    private LocalDate endDate;

    /** ID tổ đội cấu hình lại */
    private Long teamId;

    /** Ghi chú / lý do điều chỉnh */
    private String note;
}
