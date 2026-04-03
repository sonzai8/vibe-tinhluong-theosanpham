package com.plywood.payroll.modules.payroll.controller;

import com.plywood.payroll.modules.payroll.dto.response.PayrollItemResponse;
import com.plywood.payroll.modules.payroll.dto.response.PayrollResponse;
import com.plywood.payroll.modules.payroll.dto.response.PayrollDailyDetailResponse;
import com.plywood.payroll.modules.payroll.dto.response.TeamWageResponse;
import com.plywood.payroll.modules.payroll.service.PayrollService;
import com.plywood.payroll.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payrolls")
@RequiredArgsConstructor
@Tag(name = "Payroll Management", description = "Quản lý bảng lương và tính lương")
public class PayrollController {

    private final PayrollService payrollService;

    @PostMapping("/{year}/{month}/calculate")
    @Operation(summary = "Tính toán bảng lương cho tháng/năm")
    public ResponseEntity<ApiResponse<PayrollResponse>> calculate(
            @PathVariable("year") int year,
            @PathVariable("month") int month) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đang tính toán lương tháng " + month + "/" + year,
                payrollService.calculatePayroll(month, year)
        ));
    }

    @GetMapping("/{year}/{month}/items")
    @Operation(summary = "Lấy danh sách các bản ghi lương trong tháng")
    public ResponseEntity<ApiResponse<List<PayrollItemResponse>>> getItems(
            @PathVariable("year") int year,
            @PathVariable("month") int month) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách lương tháng " + month + "/" + year,
                payrollService.getPayrollItems(month, year)
        ));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Xác nhận/Chốt bảng lương")
    public ResponseEntity<ApiResponse<PayrollResponse>> confirm(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã chốt bảng lương",
                payrollService.confirmPayroll(id)
        ));
    }

    @GetMapping("/items/{id}/daily-details")
    @Operation(summary = "Lấy chi tiết lương hàng ngày của một nhân viên")
    public ResponseEntity<ApiResponse<List<PayrollDailyDetailResponse>>> getDetails(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy chi tiết lương hàng ngày",
                payrollService.getDailyDetails(id)
        ));
    }

    @PostMapping("/{year}/{month}/confirm-by-team")
    @Operation(summary = "Xác nhận lương theo tổ")
    public ResponseEntity<ApiResponse<Void>> confirmByTeam(
            @PathVariable("year") int year,
            @PathVariable("month") int month,
            @RequestParam("teamId") Long teamId) {
        payrollService.confirmByTeam(year, month, teamId);
        return ResponseEntity.ok(ApiResponse.success("Đã xác nhận lương cho tổ", null));
    }

    @GetMapping("/{year}/{month}/export-payslips")
    @Operation(summary = "Xuất file Excel phiếu lương cho toàn bộ nhân viên")
    public ResponseEntity<ByteArrayResource> exportExcel(
            @PathVariable("year") int year,
            @PathVariable("month") int month) {
        // Mocking excel export bytes - in real case call service method
        byte[] excelBytes = new byte[0];
        ByteArrayResource resource = new ByteArrayResource(excelBytes);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=PhieuLuong_" + month + "_" + year + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(excelBytes.length)
                .body(resource);
    }

    @DeleteMapping("/{year}/{month}/reset")
    @Operation(summary = "Xóa toàn bộ dữ liệu làm lương của một tháng nếu chưa chốt")
    public ResponseEntity<ApiResponse<Void>> deleteMonthlyData(
            @PathVariable("year") int year,
            @PathVariable("month") int month) {
        payrollService.resetMonthlyData(month, year);
        return ResponseEntity.ok(ApiResponse.success(
                "Đã xóa toàn bộ dữ liệu tháng " + month + "/" + year + " để tính toán lại.",
                null
        ));
    }

    @GetMapping("/{year}/{month}/team-wages")
    @Operation(summary = "Lấy chi tiết thu nhập/chi phí của các tổ theo ngày")
    public ResponseEntity<ApiResponse<List<TeamWageResponse>>> getTeamWages(
            @PathVariable("year") int year,
            @PathVariable("month") int month) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy dữ liệu thu nhập tổ tháng " + month + "/" + year,
                payrollService.getTeamWages(month, year)
        ));
    }
}
