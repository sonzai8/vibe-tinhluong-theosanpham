package com.plywood.payroll.modules.penalty.dto.response;

import com.plywood.payroll.modules.penalty.entity.PenaltyBonusType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PenaltyBonusResponse {
    private Long id;
    private Long employeeId;
    private String employeeFullName;
    private String employeeCode;
    private Long employeeDepartmentId;
    private Long employeeTeamId;
    private LocalDate recordDate;
    private PenaltyBonusType type;
    private BigDecimal amount;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
