package com.plywood.payroll.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PayrollItemResponse {
    private Long id;
    private PayrollResponse payroll;
    private EmployeeResponse employee;
    private BigDecimal totalStepSalary;
    private BigDecimal totalBenefit;
    private BigDecimal totalBonus;
    private BigDecimal totalPenalty;
    private BigDecimal netSalary;
}
