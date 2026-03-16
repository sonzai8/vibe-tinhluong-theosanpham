package com.plywood.payroll.config;

import com.plywood.payroll.constant.PermissionConstants;
import com.plywood.payroll.entity.*;
import com.plywood.payroll.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final ProductionStepRepository productionStepRepository;
    private final TeamRepository teamRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public static final String DEFAULT_DEPT = "Phòng Chờ";
    public static final String DEFAULT_STEP = "Công Đoạn Chờ";
    public static final String DEFAULT_TEAM = "Team Chờ";
    public static final String DEFAULT_ROLE = "Nhân Viên";
    public static final String ADMIN_ROLE = "Supper Admin";
    public static final String ADMIN_USER = "supperadmin";
    public static final String ADMIN_PASS = "supperadmin";

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking and initializing default database data...");

        // 1. Department
        Department defDept = departmentRepository.findAll().stream()
                .filter(d -> d.getName().equals(DEFAULT_DEPT))
                .findFirst()
                .orElseGet(() -> {
                    Department d = new Department();
                    d.setName(DEFAULT_DEPT);
                    return departmentRepository.save(d);
                });

        // 2. ProductionStep (Required for Team)
        ProductionStep defStep = productionStepRepository.findAll().stream()
                .filter(s -> s.getName().equals(DEFAULT_STEP))
                .findFirst()
                .orElseGet(() -> {
                    ProductionStep s = new ProductionStep();
                    s.setName(DEFAULT_STEP);
                    return productionStepRepository.save(s);
                });

        // 3. Team
        teamRepository.findAll().stream()
                .filter(t -> t.getName().equals(DEFAULT_TEAM))
                .findFirst()
                .orElseGet(() -> {
                    Team t = new Team();
                    t.setName(DEFAULT_TEAM);
                    t.setDepartment(defDept);
                    t.setProductionStep(defStep);
                    return teamRepository.save(t);
                });

        // 4. Role
        roleRepository.findAll().stream()
                .filter(r -> r.getName().equals(DEFAULT_ROLE))
                .findFirst()
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setName(DEFAULT_ROLE);
                    r.setDailyBenefit(BigDecimal.ZERO);
                    return roleRepository.save(r);
                });

        // 5. Supper Admin Role
        Role adminRole = roleRepository.findAll().stream()
                .filter(r -> r.getName().equals(ADMIN_ROLE))
                .findFirst()
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setName(ADMIN_ROLE);
                    r.setDailyBenefit(BigDecimal.ZERO);
                    
                    // Gán toàn bộ quyền cho Supper Admin
                    Set<String> perms = new HashSet<>();
                    perms.add(PermissionConstants.EMPLOYEE_VIEW);
                    perms.add(PermissionConstants.EMPLOYEE_EDIT);
                    perms.add(PermissionConstants.PAYROLL_VIEW);
                    perms.add(PermissionConstants.PAYROLL_EDIT);
                    perms.add(PermissionConstants.ATTENDANCE_VIEW);
                    perms.add(PermissionConstants.ATTENDANCE_EDIT);
                    perms.add(PermissionConstants.PRODUCTION_VIEW);
                    perms.add(PermissionConstants.PRODUCTION_EDIT);
                    perms.add(PermissionConstants.SYSTEM_ADMIN);
                    
                    r.setPermissions(perms);
                    return roleRepository.save(r);
                });

        // 6. Default Admin Account
        if (employeeRepository.findByUsername(ADMIN_USER).isEmpty() && !employeeRepository.existsByCode("ADMIN")) {
            Employee admin = new Employee();
            admin.setCode("ADMIN");
            admin.setFullName("Quản trị viên hệ thống");
            admin.setUsername(ADMIN_USER);
            admin.setPassword(passwordEncoder.encode(ADMIN_PASS));
            admin.setStatus("ACTIVE");
            admin.setCanLogin(true);
            admin.setDepartment(defDept);
            admin.setRole(adminRole);
            
            // Tìm team mặc định để gán
            teamRepository.findAll().stream()
                .filter(t -> t.getName().equals(DEFAULT_TEAM))
                .findFirst().ifPresent(admin::setTeam);

            employeeRepository.save(admin);
            log.info("Supper Admin account created: {}/{}", ADMIN_USER, ADMIN_PASS);
        }

        log.info("Default database data initialized successfully.");
    }
}
