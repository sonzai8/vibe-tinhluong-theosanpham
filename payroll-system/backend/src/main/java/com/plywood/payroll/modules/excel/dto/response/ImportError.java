package com.plywood.payroll.modules.excel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportError {
    private int rowNumber;
    private String columnName;
    private String cellValue;
    private String errorMessage;
}
