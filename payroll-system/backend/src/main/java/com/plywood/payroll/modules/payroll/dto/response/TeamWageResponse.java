package com.plywood.payroll.modules.payroll.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamWageResponse {
    private Long teamId;
    private String teamName;
    private LocalDate date;
    
    // Tổng giá trị sản phẩm tổ làm ra (Fund của tổ)
    private BigDecimal totalTeamIncome;
    
    // Quỹ đầu chuyền (trừ vào doanh thu trước khi chia)
    private BigDecimal leadFundAmount;
    
    // Chi phí nhân sự (lương trả cho công nhân)
    private BigDecimal internalLaborCost;  // Lương của người trong tổ
    private BigDecimal borrowedLaborCost;  // Lương của người mượn từ tổ khác
    
    // Thu nhập từ việc tổ đi làm thuê cho tổ khác
    private BigDecimal lendLaborCost;

    private List<WorkerWageDetail> details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkerWageDetail {
        private Long employeeId;
        private String employeeName;
        private String employeeCode;
        private String originalTeamName;
        private String actualTeamName;
        private BigDecimal amount;
        private boolean isBorrowed; // true nếu người này từ tổ khác đến
        private String status; // INTERNAL, BORROWED, LEND
    }
}
