package com.plywood.payroll.config;

import com.plywood.payroll.entity.Department;
import com.plywood.payroll.entity.ProductionStep;
import com.plywood.payroll.entity.Role;
import com.plywood.payroll.entity.Team;
import com.plywood.payroll.repository.DepartmentRepository;
import com.plywood.payroll.repository.ProductionStepRepository;
import com.plywood.payroll.repository.RoleRepository;
import com.plywood.payroll.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final ProductionStepRepository productionStepRepository;
    private final TeamRepository teamRepository;
    private final RoleRepository roleRepository;

    public static final String DEFAULT_DEPT = "Phòng Chờ";
    public static final String DEFAULT_STEP = "Công Đoạn Chờ";
    public static final String DEFAULT_TEAM = "Team Chờ";
    public static final String DEFAULT_ROLE = "Nhân Viên";

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

        log.info("Default database data initialized successfully.");
    }
}
