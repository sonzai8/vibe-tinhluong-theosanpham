package com.plywood.payroll.security;

import com.plywood.payroll.constant.MessageConstants;
import com.plywood.payroll.entity.Employee;
import com.plywood.payroll.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Employee employee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(String.format(MessageConstants.ERR_RESOURCE_NOT_FOUND, "tài khoản", 0L)));

        if (!employee.isCanLogin()) {
            throw new DisabledException("Tài khoản này không có quyền đăng nhập hệ thống");
        }

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (employee.getRole() != null) {
            // Thêm Role (ví dụ: ROLE_ADMIN, ROLE_CONG_NHAN)
            authorities.add(new SimpleGrantedAuthority("ROLE_" + employee.getRole().getName().toUpperCase()));
            
            // Thêm các quyền chi tiết (ví dụ: EMPLOYEE_VIEW, PAYROLL_EDIT)
            if (employee.getRole().getPermissions() != null) {
                authorities.addAll(employee.getRole().getPermissions().stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList()));
            }
        }

        return new User(employee.getUsername(), employee.getPassword(), authorities);
    }
}
