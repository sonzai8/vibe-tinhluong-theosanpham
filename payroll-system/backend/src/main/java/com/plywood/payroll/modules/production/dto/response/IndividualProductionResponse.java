package com.plywood.payroll.modules.production.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndividualProductionResponse {
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private LocalDate date;
    private Long actualTeamId;
    private String actualTeamName;
    private Double quantity; // Sản lượng bình quân (Tổng tổ / Số người)
}
