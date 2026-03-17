package com.plywood.payroll.modules.auth.controller;

import com.plywood.payroll.shared.constant.MessageConstants;
import com.plywood.payroll.modules.auth.dto.request.LoginRequest;
import com.plywood.payroll.modules.auth.dto.request.RegisterRequest;
import com.plywood.payroll.shared.dto.ApiResponse;
import com.plywood.payroll.modules.auth.dto.response.LoginResponse;
import com.plywood.payroll.modules.employee.entity.Employee;
import com.plywood.payroll.shared.exception.BusinessException;
import com.plywood.payroll.modules.employee.repository.EmployeeRepository;
import com.plywood.payroll.modules.organization.repository.DepartmentRepository;
import com.plywood.payroll.modules.organization.repository.TeamRepository;
import com.plywood.payroll.modules.organization.repository.RoleRepository;
import com.plywood.payroll.shared.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Quản lý đăng nhập và xác thực")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final RoleRepository roleRepository;

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập hệ thống", description = "Đăng nhập với username và password để nhận JWT Token")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        String token = jwtUtil.generateToken(username);
        Employee emp = employeeRepository.findByUsername(username).orElseThrow();

        if (!emp.isCanLogin()) {
            throw new BusinessException("Tài khoản này không có quyền đăng nhập hệ thống");
        }

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setEmployeeId(emp.getId());
        response.setFullName(emp.getFullName());
        if (emp.getRole() != null) {
            response.setRoleName(emp.getRole().getName());
            response.setPermissions(emp.getRole().getPermissions());
        }

        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_LOGIN, response));
    }

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản", description = "Tạo một nhân viên (tài khoản) mới")
    public ResponseEntity<ApiResponse<Employee>> register(@Valid @RequestBody RegisterRequest request) {
        if (employeeRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BusinessException(MessageConstants.ERR_USERNAME_EXISTED);
        }

        Employee emp = new Employee();
        emp.setCode(request.getCode());
        emp.setUsername(request.getUsername());
        emp.setPassword(passwordEncoder.encode(request.getPassword()));
        emp.setFullName(request.getFullName());
        emp.setStatus("ACTIVE");

        // Gán dữ liệu mặc định
        departmentRepository.findAll().stream()
                .filter(d -> d.getName().equals("Mặc định"))
                .findFirst().ifPresent(emp::setDepartment);

        teamRepository.findAll().stream()
                .filter(t -> t.getName().equals("Mặc định"))
                .findFirst().ifPresent(emp::setTeam);

        roleRepository.findAll().stream()
                .filter(r -> r.getName().equals("Nhân viên"))
                .findFirst().ifPresent(emp::setRole);

        employeeRepository.save(emp);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.SUCCESS_REGISTER, emp));
    }
}
