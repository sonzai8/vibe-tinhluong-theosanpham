package com.plywood.payroll.shared.config;
import com.plywood.payroll.modules.organization.repository.TeamRepository;
import com.plywood.payroll.modules.organization.repository.RoleRepository;
import com.plywood.payroll.modules.production.repository.ProductionStepRepository;
import com.plywood.payroll.modules.organization.repository.DepartmentRepository;
import com.plywood.payroll.modules.employee.repository.EmployeeRepository;
import com.plywood.payroll.modules.production.entity.ProductionStep;
import com.plywood.payroll.modules.organization.entity.Role;
import com.plywood.payroll.modules.employee.entity.SalaryProcess;
import com.plywood.payroll.modules.employee.entity.TeamProcess;
import com.plywood.payroll.modules.employee.repository.SalaryProcessRepository;
import com.plywood.payroll.modules.employee.repository.TeamProcessRepository;
import com.plywood.payroll.modules.organization.entity.Department;
import com.plywood.payroll.modules.organization.entity.Team;
import com.plywood.payroll.modules.employee.entity.Employee;

import com.plywood.payroll.shared.constant.PermissionConstants;
// REMOVED_WILDCARD_ENTITY_IMPORT - please update manually
// REMOVED_WILDCARD_REPO_IMPORT - please update manually
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final SalaryProcessRepository salaryProcessRepository;
    private final TeamProcessRepository teamProcessRepository;
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
            
            Employee savedAdmin = employeeRepository.save(admin);

            // Seeding Initial History for Admin
            SalaryProcess sp = new SalaryProcess();
            sp.setEmployee(savedAdmin);
            sp.setSalaryType(com.plywood.payroll.modules.employee.entity.SalaryType.FIXED_MONTHLY);
            sp.setBaseSalary(new BigDecimal("20000000"));
            sp.setInsuranceSalary(new BigDecimal("5000000"));
            sp.setStartDate(LocalDate.now());
            salaryProcessRepository.save(sp);

            teamRepository.findAll().stream()
                .filter(t -> t.getName().equals(DEFAULT_TEAM))
                .findFirst().ifPresent(t -> {
                    TeamProcess tp = new TeamProcess();
                    tp.setEmployee(savedAdmin);
                    tp.setTeam(t);
                    tp.setStartDate(LocalDate.now());
                    teamProcessRepository.save(tp);
                });

            log.info("Supper Admin account created: {}/{}", ADMIN_USER, ADMIN_PASS);
        }

        log.info("Default database data initialized successfully.");
    }
}
