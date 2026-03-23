package com.plywood.payroll.modules.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String refreshToken;
    private Long employeeId;
    private String fullName;
    private String roleName;
    private java.util.Set<String> permissions;
}
