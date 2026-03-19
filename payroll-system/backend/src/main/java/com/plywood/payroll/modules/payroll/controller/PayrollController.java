package com.plywood.payroll.modules.payroll.controller;
import com.plywood.payroll.modules.payroll.entity.Payroll;

import com.plywood.payroll.shared.constant.MessageConstants;


import com.plywood.payroll.shared.dto.ApiResponse;
import com.plywood.payroll.modules.payroll.dto.response.PayrollDailyDetailResponse;
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
    private final com.plywood.payroll.modules.excel.service.ExcelService excelService;

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


    @GetMapping("/items/{id}/daily-details")
    @Operation(summary = "Lấy chi tiết lương hàng ngày của nhân viên")
    public ResponseEntity<ApiResponse<List<PayrollDailyDetailResponse>>> getDailyDetails(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy chi tiết lương hàng ngày",
                payrollService.getDailyDetails(id)
        ));
    }

    @PutMapping("/{year}/{month}/confirm-team/{teamId}")
    @Operation(summary = "Xác nhận chốt bảng lương theo tổ")
    public ResponseEntity<ApiResponse<Void>> confirmByTeam(
            @PathVariable("year") int year,
            @PathVariable("month") int month,
            @PathVariable("teamId") Long teamId) {
        payrollService.confirmByTeam(year, month, teamId);
        return ResponseEntity.ok(ApiResponse.success("Đã chốt bảng lương cho tổ", null));
    }

    @GetMapping("/{year}/{month}/export-payslips")
    @Operation(summary = "Xuất phiếu lương tháng ra Excel (mỗi nhân viên 1 sheet)")
    public ResponseEntity<org.springframework.core.io.Resource> exportPayslips(
            @PathVariable("year") int year,
            @PathVariable("month") int month) {
        
        List<PayrollItemResponse> items = payrollService.getPayrollItems(month, year);
        java.util.Map<Long, List<PayrollDailyDetailResponse>> detailsMap = new java.util.HashMap<>();
        for (PayrollItemResponse item : items) {
            detailsMap.put(item.getId(), payrollService.getDailyDetails(item.getId()));
        }

        try {
            byte[] data = excelService.exportPayslips(month, year, items, detailsMap);
            org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(data);
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"PhieuLuong_" + month + "_" + year + ".xlsx\"")
                    .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(resource);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Lỗi khi xuất file Excel phiếu lương", e);
        }
    }
}
