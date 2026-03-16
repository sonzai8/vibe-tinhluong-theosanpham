package com.plywood.payroll.controller;

import com.plywood.payroll.constant.MessageConstants;


import com.plywood.payroll.dto.request.DepartmentRequest;
import com.plywood.payroll.dto.response.ApiResponse;
import com.plywood.payroll.dto.response.DepartmentResponse;
import com.plywood.payroll.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Tag(name = "Department", description = "Quản lý phòng ban")
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả phòng ban")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, departmentService.getAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin phòng ban theo ID")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_DETAIL, departmentService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Tạo mới phòng ban")
    public ResponseEntity<ApiResponse<DepartmentResponse>> create(@Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.SUCCESS_CREATE, departmentService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật phòng ban")
    public ResponseEntity<ApiResponse<DepartmentResponse>> update(@PathVariable("id") Long id, @Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_UPDATE, departmentService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa phòng ban")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        departmentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_DELETE, null));
    }
}
