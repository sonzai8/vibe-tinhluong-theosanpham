package com.plywood.payroll.modules.penalty.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PenaltyBonusSummaryResponse {
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String departmentName;
    private String teamName;
    private BigDecimal totalPenalty;
    private BigDecimal totalBonus;
    private BigDecimal netAmount;
    private long penaltyCount;
    private long bonusCount;
}
