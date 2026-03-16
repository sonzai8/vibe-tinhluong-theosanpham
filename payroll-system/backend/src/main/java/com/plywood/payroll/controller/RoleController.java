package com.plywood.payroll.controller;

import com.plywood.payroll.constant.MessageConstants;


import com.plywood.payroll.dto.request.RoleRequest;
import com.plywood.payroll.dto.response.ApiResponse;
import com.plywood.payroll.dto.response.RoleResponse;
import com.plywood.payroll.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Tag(name = "Role", description = "Quản lý chức vụ và phụ cấp chức vụ")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "Lấy danh sách chức vụ")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, roleService.getAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin chức vụ theo ID")
    public ResponseEntity<ApiResponse<RoleResponse>> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_DETAIL, roleService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Tạo mới chức vụ")
    public ResponseEntity<ApiResponse<RoleResponse>> create(@Valid @RequestBody RoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.SUCCESS_CREATE, roleService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật chức vụ")
    public ResponseEntity<ApiResponse<RoleResponse>> update(@PathVariable("id") Long id, @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_UPDATE, roleService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa chức vụ")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        roleService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_DELETE, null));
    }
}
