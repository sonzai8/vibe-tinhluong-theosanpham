package com.plywood.payroll.modules.payroll.controller;
import com.plywood.payroll.modules.payroll.entity.Payroll;

import com.plywood.payroll.shared.constant.MessageConstants;


import com.plywood.payroll.shared.dto.ApiResponse;
import com.plywood.payroll.modules.payroll.dto.response.PayrollItemResponse;
import com.plywood.payroll.modules.payroll.dto.response.PayrollResponse;
import com.plywood.payroll.modules.payroll.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payrolls")
@RequiredArgsConstructor
@Tag(name = "Payroll", description = "Quản lý Bảng lương")
public class PayrollController {

    private final PayrollService payrollService;

    @PostMapping("/calculate")
    @Operation(summary = "Tính toán lại lương tháng")
    public ResponseEntity<ApiResponse<PayrollResponse>> calculatePayroll(@RequestBody Map<String, Integer> body) {
        int month = body.get("month");
        int year = body.get("year");
        return ResponseEntity.ok(ApiResponse.success(
                MessageConstants.SUCCESS_CALCULATE_PAYROLL + " cho tháng " + month + "/" + year,
                payrollService.calculatePayroll(month, year)
        ));
    }

    @GetMapping("/{year}/{month}/items")
    @Operation(summary = "Lấy chi tiết bảng lương từng người trong tháng")
    public ResponseEntity<ApiResponse<List<PayrollItemResponse>>> getPayrollItems(
            @PathVariable("month") int month,
            @PathVariable("year") int year) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy chi tiết lương tháng " + month + "/" + year,
                payrollService.getPayrollItems(month, year)
        ));
    }
    
    @PutMapping("/{id}/confirm")
    @Operation(summary = "Xác nhận chốt bảng lương")
    public ResponseEntity<ApiResponse<PayrollResponse>> confirmPayroll(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã chốt bảng lương",
                payrollService.confirmPayroll(id)
        ));
    }
}
