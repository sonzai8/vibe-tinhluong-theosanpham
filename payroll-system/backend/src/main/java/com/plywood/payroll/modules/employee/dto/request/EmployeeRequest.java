package com.plywood.payroll.modules.employee.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmployeeRequest {
    @NotBlank(message = "Mã nhân viên không hợp lệ")
    private String code;

    @NotBlank(message = "Họ tên không hợp lệ")
    private String fullName;

    private Long departmentId;

    // Tổ biên chế trực tiếp (thêm mới)
    private Long teamId;

    // roleId = chức vụ (Position), giữ nguyên để backward compat
    private Long roleId;

    private String status = "ACTIVE";

    private String phone;
    private String citizenId;

    private String username;

    private String password;

    private boolean canLogin = false;
}
