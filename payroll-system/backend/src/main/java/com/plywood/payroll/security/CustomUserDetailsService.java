package com.plywood.payroll.security;

import com.plywood.payroll.constant.MessageConstants;
import com.plywood.payroll.entity.Employee;
import com.plywood.payroll.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Employee employee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(String.format(MessageConstants.ERR_RESOURCE_NOT_FOUND, "tài khoản", 0L))); // Username passed in exception context if needed

        return new User(employee.getUsername(), employee.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
