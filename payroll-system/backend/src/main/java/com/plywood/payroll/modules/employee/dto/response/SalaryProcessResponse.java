package com.plywood.payroll.modules.employee.dto.response;

import com.plywood.payroll.modules.employee.entity.SalaryType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SalaryProcessResponse {
    private Long id;
    private SalaryType salaryType;
    private BigDecimal baseSalary;
    private BigDecimal insuranceSalary;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isCurrent;
    private String employeeCode;
    private String employeeName;
    private Long employeeId;
    private String teamName;
}
