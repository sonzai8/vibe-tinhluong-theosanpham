package com.plywood.payroll.modules.employee.dto.request;

import com.plywood.payroll.modules.employee.entity.SalaryType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SalaryProcessRequest {
    private Long employeeId;
    private SalaryType salaryType;
    private BigDecimal baseSalary;
    private BigDecimal insuranceSalary;
    private LocalDate startDate;
    private LocalDate endDate;
}
