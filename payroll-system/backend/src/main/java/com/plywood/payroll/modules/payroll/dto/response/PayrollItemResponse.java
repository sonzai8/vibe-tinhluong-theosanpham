package com.plywood.payroll.modules.payroll.dto.response;
import com.plywood.payroll.modules.employee.dto.response.EmployeeResponse;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PayrollItemResponse {
    private Long id;
    private Long payrollId;
    private Integer month;
    private Integer year;
    private String status;
    private String employeeName;
    private String employeeCode;
    private Long departmentId;
    private String departmentName;
    private Long teamId;
    private String teamName;
    private BigDecimal productSalary;
    private BigDecimal benefitSalary;
    private BigDecimal totalPenaltyBonus;
    private BigDecimal totalSalary;

    private BigDecimal insuranceSalary;
    private BigDecimal cashSalary;
}
