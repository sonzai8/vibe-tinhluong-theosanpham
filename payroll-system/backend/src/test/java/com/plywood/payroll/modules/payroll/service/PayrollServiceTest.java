package com.plywood.payroll.modules.payroll.service;

import com.plywood.payroll.modules.attendance.entity.DailyAttendance;
import com.plywood.payroll.modules.attendance.repository.DailyAttendanceRepository;
import com.plywood.payroll.modules.employee.entity.Employee;
import com.plywood.payroll.modules.employee.repository.EmployeeRepository;
import com.plywood.payroll.modules.production.entity.ProductionStep;
import com.plywood.payroll.modules.organization.entity.Team;
import com.plywood.payroll.modules.payroll.entity.Payroll;
import com.plywood.payroll.modules.payroll.repository.PayrollConfigRepository;
import com.plywood.payroll.modules.payroll.repository.PayrollItemRepository;
import com.plywood.payroll.modules.payroll.repository.PayrollRepository;
import com.plywood.payroll.modules.penalty.repository.PenaltyBonusRepository;
import com.plywood.payroll.modules.pricing.entity.ProductStepRate;
import com.plywood.payroll.modules.pricing.repository.ProductStepRateRepository;
import com.plywood.payroll.modules.product.entity.Product;
import com.plywood.payroll.modules.production.entity.ProductionRecord;
import com.plywood.payroll.modules.production.repository.ProductionRecordRepository;
import com.plywood.payroll.modules.quality.entity.ProductQuality;
import org.springframework.data.jpa.domain.Specification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock private PayrollRepository payrollRepository;
    @Mock private PayrollItemRepository payrollItemRepository;
    @Mock private ProductionRecordRepository productionRecordRepository;
    @Mock private ProductStepRateRepository productStepRateRepository;
    @Mock private DailyAttendanceRepository dailyAttendanceRepository;
    @Mock private PayrollConfigRepository payrollConfigRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private PenaltyBonusRepository penaltyBonusRepository;

    @InjectMocks
    private PayrollService payrollService;

    private Employee employee;
    private Team team;
    private Product product;
    private ProductQuality quality;
    private ProductionStep step;

    @BeforeEach
    void setUp() {
        step = new ProductionStep();
        step.setId(1L);
        step.setName("Plywood Pressing");

        team = new Team();
        team.setId(1L);
        team.setName("Team A");
        team.setProductionStep(step);

        employee = new Employee();
        employee.setId(1L);
        employee.setFullName("Nguyen Van A");
        employee.setCode("NV001");
        employee.setTeam(team);

        product = new Product();
        product.setId(1L);
        product.setCode("PROD01");

        quality = new ProductQuality();
        quality.setId(1L);
        quality.setCode("A1");
    }

    @Test
    void testCalculatePayroll_BasicSuccess() {
        int month = 10;
        int year = 2023;
        LocalDate date = LocalDate.of(year, month, 5);

        // Mock Payroll
        Payroll payroll = new Payroll();
        payroll.setId(1L);
        payroll.setMonth(month);
        payroll.setYear(year);
        payroll.setStatus("DRAFT");
        when(payrollRepository.findByMonthAndYear(month, year)).thenReturn(Optional.of(payroll));

        // Mock Configs
        when(payrollConfigRepository.findEffectiveConfig(anyString(), anyInt(), anyInt())).thenReturn(Optional.empty());

        // Mock Production Records
        ProductionRecord record = new ProductionRecord();
        record.setTeam(team);
        record.setProduct(product);
        record.setQuality(quality);
        record.setProductionDate(date);
        record.setQuantity(100);
        when(productionRecordRepository.findAll(any(Specification.class))).thenReturn(Collections.singletonList(record));

        // Mock Attendance
        DailyAttendance att = new DailyAttendance();
        att.setEmployee(employee);
        att.setActualTeam(team);
        att.setAttendanceDate(date);
        when(dailyAttendanceRepository.findByFilters(any(), any(), anyInt(), anyInt(), any(), any(), anyBoolean(), any(), anyBoolean())).thenReturn(Collections.singletonList(att));

        // Mock Rates
        ProductStepRate rate = new ProductStepRate();
        rate.setPriceHigh(new BigDecimal("1000"));
        rate.setPriceLow(new BigDecimal("800"));
        when(productStepRateRepository.findEffectiveRate(anyLong(), anyLong(), anyLong(), any())).thenReturn(Optional.of(rate));

        // Mock Employee check
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(payrollItemRepository.findByPayrollIdAndEmployeeId(anyLong(), anyLong())).thenReturn(Optional.empty());

        // Execute
        payrollService.calculatePayroll(month, year);

        // Verify
        verify(payrollItemRepository, atLeastOnce()).save(any());
    }

    @Test
    void testCalculatePayroll_AlreadyConfirmed() {
        int month = 10;
        int year = 2023;

        Payroll payroll = new Payroll();
        payroll.setStatus("CONFIRMED");
        when(payrollRepository.findByMonthAndYear(month, year)).thenReturn(Optional.of(payroll));

        assertThrows(RuntimeException.class, () -> payrollService.calculatePayroll(month, year));
    }
}
