package com.plywood.payroll.modules.employee.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmployeeRequest {
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

    private String gender;
    private java.time.LocalDate dob;
    private java.time.LocalDate joinDate;
    private java.time.LocalDate insuranceStartDate;
    private java.time.LocalDate citizenIdIssuedDate;
    private String citizenIdIssuedPlace;
    private String birthAddress;
    private String permanentAddress;
    private String notes;

    private String username;

    private String password;

    private boolean canLogin = false;
}
