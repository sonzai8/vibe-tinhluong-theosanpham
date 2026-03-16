package com.plywood.payroll.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmployeeRequest {
    @NotBlank(message = "Mã nhân viên không hợp lệ")
    private String code;

    @NotBlank(message = "Họ tên không hợp lệ")
    private String fullName;

    private Long departmentId;
    
    private Long roleId;

    private String status = "ACTIVE";
    
    private String username;
    
    private String password;
}
