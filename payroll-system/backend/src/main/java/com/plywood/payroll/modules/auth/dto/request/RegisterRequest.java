package com.plywood.payroll.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Mã nhân viên không được để trống")
    private String code;
    
    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String username;
    
    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
    
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;
}
