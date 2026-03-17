package com.plywood.payroll.modules.organization.controller;
import com.plywood.payroll.modules.excel.service.ExcelService;
import com.plywood.payroll.modules.organization.entity.Role;

import com.plywood.payroll.shared.constant.MessageConstants;


import com.plywood.payroll.modules.organization.dto.request.RoleRequest;
import com.plywood.payroll.shared.dto.ApiResponse;
import com.plywood.payroll.modules.organization.dto.response.RoleResponse;
import com.plywood.payroll.modules.organization.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Xóa chức vụ")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        roleService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_DELETE, null));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Xuất danh sách chức vụ ra Excel")
    public ResponseEntity<byte[]> export() throws java.io.IOException {
        List<RoleResponse> list = roleService.getAll();
        String[] headers = {"ID", "Tên chức vụ", "Phụ cấp ngày", "Quyền hạn", "Ngày tạo"};
        List<List<String>> data = list.stream().map(r -> java.util.Arrays.asList(
                r.getId().toString(),
                r.getName(),
                r.getDailyBenefit().toString(),
                r.getPermissions() != null ? String.join(", ", r.getPermissions()) : "",
                r.getCreatedAt() != null ? r.getCreatedAt().toString() : ""
        )).collect(java.util.stream.Collectors.toList());

        byte[] bytes = excelService.exportGeneric("ChucVu", headers, data);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=danh_sach_chuc_vu.xlsx")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @GetMapping("/download-template")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Tải file mẫu nhập chức vụ")
    public ResponseEntity<byte[]> downloadTemplate() throws java.io.IOException {
        String[] headers = {"Tên chức vụ", "Phụ cấp ngày"};
        byte[] bytes = excelService.getGenericTemplate("MauNhap", headers);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mau_nhap_chuc_vu.xlsx")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Nhập danh sách chức vụ từ Excel")
    public ResponseEntity<ApiResponse<Void>> importExcel(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        List<RoleRequest> requests = excelService.importRoles(file);
        for (RoleRequest req : requests) {
            roleService.create(req);
        }
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_CREATE, null));
    }

    private final com.plywood.payroll.modules.excel.service.ExcelService excelService;
}
