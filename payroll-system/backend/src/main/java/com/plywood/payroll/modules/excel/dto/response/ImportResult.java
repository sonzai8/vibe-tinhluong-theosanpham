package com.plywood.payroll.modules.excel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult<T> {
    private int successCount;
    private int errorCount;
    private List<ImportError> errors;
    private List<T> data;
}
