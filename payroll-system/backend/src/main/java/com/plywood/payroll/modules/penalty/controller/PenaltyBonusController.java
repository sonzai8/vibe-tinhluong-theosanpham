package com.plywood.payroll.modules.penalty.controller;

import com.plywood.payroll.shared.constant.MessageConstants;


import com.plywood.payroll.modules.penalty.dto.request.PenaltyBonusRequest;
import com.plywood.payroll.shared.dto.ApiResponse;
import com.plywood.payroll.modules.penalty.dto.response.PenaltyBonusResponse;
import com.plywood.payroll.modules.penalty.dto.response.PenaltyBonusSummaryResponse;
import com.plywood.payroll.modules.penalty.service.PenaltyBonusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/penalty-bonuses")
@RequiredArgsConstructor
@Tag(name = "Penalty Bonus", description = "Quản lý khen thưởng và kỷ luật")
public class PenaltyBonusController {

    private final PenaltyBonusService penaltyBonusService;

    @GetMapping
    @Operation(summary = "Danh sách tất cả khoản khen thưởng/kỷ luật")
    public ResponseEntity<ApiResponse<List<PenaltyBonusResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, penaltyBonusService.getAll()));
    }
    
    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Danh sách khen thưởng/kỷ luật của một nhân viên")
    public ResponseEntity<ApiResponse<List<PenaltyBonusResponse>>> getByEmployee(@PathVariable("employeeId") Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, penaltyBonusService.getByEmployee(employeeId)));
    }

    @PostMapping
    @Operation(summary = "Tạo khoản khen thưởng/kỷ luật mới (Số tiền dương = thưởng, âm = phạt)")
    public ResponseEntity<ApiResponse<PenaltyBonusResponse>> create(@Valid @RequestBody PenaltyBonusRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.SUCCESS_CREATE, penaltyBonusService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật khoản khen thưởng/kỷ luật")
    public ResponseEntity<ApiResponse<PenaltyBonusResponse>> update(@PathVariable("id") Long id, @Valid @RequestBody PenaltyBonusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_UPDATE, penaltyBonusService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa khoản khen thưởng/kỷ luật")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        penaltyBonusService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_DELETE, null));
    }

    @GetMapping("/summary")
    @Operation(summary = "Tổng hợp khen thưởng/kỷ luật theo thời gian và đơn vị")
    public ResponseEntity<ApiResponse<List<PenaltyBonusSummaryResponse>>> getSummary(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "teamId", required = false) Long teamId) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, 
                penaltyBonusService.getSummary(startDate, endDate, departmentId, teamId)));
    }

    @GetMapping("/employee/{employeeId}/stats")
    @Operation(summary = "Thống kê khen thưởng/kỷ luật của một nhân viên")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEmployeeStats(@PathVariable("employeeId") Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_DETAIL, 
                penaltyBonusService.getEmployeeStats(employeeId)));
    }
}
