package com.plywood.payroll.controller;

import com.plywood.payroll.constant.MessageConstants;


import com.plywood.payroll.dto.request.DailyAttendanceRequest;
import com.plywood.payroll.dto.response.ApiResponse;
import com.plywood.payroll.dto.response.DailyAttendanceResponse;
import com.plywood.payroll.service.DailyAttendanceService;
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

@RestController
@RequestMapping("/api/attendances")
@RequiredArgsConstructor
@Tag(name = "Daily Attendance", description = "Quản lý điểm danh hàng ngày")
public class DailyAttendanceController {

    private final DailyAttendanceService attendanceService;

    @GetMapping
    @Operation(summary = "Danh sách điểm danh trong tháng/năm")
    public ResponseEntity<ApiResponse<List<DailyAttendanceResponse>>> getByMonthAndYear(
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, attendanceService.getByMonthAndYear(month, year)));
    }
    
    @GetMapping("/date/{date}")
    @Operation(summary = "Danh sách điểm danh theo ngày")
    public ResponseEntity<ApiResponse<List<DailyAttendanceResponse>>> getByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, attendanceService.getByDate(date)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy điểm danh theo ID")
    public ResponseEntity<ApiResponse<DailyAttendanceResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_DETAIL, attendanceService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Thêm một bản ghi điểm danh")
    public ResponseEntity<ApiResponse<DailyAttendanceResponse>> create(@Valid @RequestBody DailyAttendanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.SUCCESS_CREATE, attendanceService.create(request)));
    }
    
    @PostMapping("/batch")
    @Operation(summary = "Thêm nhiều bản ghi điểm danh cùng lúc")
    public ResponseEntity<ApiResponse<List<DailyAttendanceResponse>>> createBatch(@RequestBody List<@Valid DailyAttendanceRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.SUCCESS_CREATE, attendanceService.saveBatch(requests)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật bản ghi điểm danh")
    public ResponseEntity<ApiResponse<DailyAttendanceResponse>> update(@PathVariable Long id, @Valid @RequestBody DailyAttendanceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_UPDATE, attendanceService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa điểm danh")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        attendanceService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_DELETE, null));
    }
}
