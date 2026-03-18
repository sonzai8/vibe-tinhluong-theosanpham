package com.plywood.payroll.modules.payroll.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class PayrollDailyDetailResponse {
    private LocalDate date;
    private String attendanceSymbol;
    private String teamName;
    private BigDecimal productSalary;
    private BigDecimal benefitSalary;
    private BigDecimal bonus;
    private BigDecimal penalty;
    private String note;
}
