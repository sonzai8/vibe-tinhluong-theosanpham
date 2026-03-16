package com.plywood.payroll.controller;

import com.plywood.payroll.entity.Employee;
import com.plywood.payroll.repository.EmployeeRepository;
import com.plywood.payroll.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final EmployeeRepository employeeRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        String token = jwtUtil.generateToken(username);
        Employee emp = employeeRepository.findByUsername(username).orElseThrow();

        return ResponseEntity.ok(Map.of(
                "token", token,
                "employeeId", emp.getId(),
                "fullName", emp.getFullName()
        ));
    }
}
