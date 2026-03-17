package com.plywood.payroll.modules.organization.controller;
import com.plywood.payroll.modules.excel.service.ExcelService;
import com.plywood.payroll.modules.organization.entity.Department;

import com.plywood.payroll.shared.constant.MessageConstants;


import com.plywood.payroll.modules.organization.dto.request.DepartmentRequest;
import com.plywood.payroll.shared.dto.ApiResponse;
import com.plywood.payroll.modules.organization.dto.response.DepartmentResponse;
import com.plywood.payroll.modules.organization.service.DepartmentService;
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
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Tag(name = "Department", description = "Quản lý phòng ban")
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Lấy danh sách tất cả phòng ban")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, departmentService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Lấy thông tin phòng ban theo ID")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_DETAIL, departmentService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Tạo mới phòng ban")
    public ResponseEntity<ApiResponse<DepartmentResponse>> create(@Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.SUCCESS_CREATE, departmentService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Cập nhật phòng ban")
    public ResponseEntity<ApiResponse<DepartmentResponse>> update(@PathVariable("id") Long id, @Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_UPDATE, departmentService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Xóa phòng ban")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        departmentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_DELETE, null));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Xuất danh sách phòng ban ra Excel")
    public ResponseEntity<byte[]> export() throws java.io.IOException {
        List<DepartmentResponse> list = departmentService.getAll();
        String[] headers = {"ID", "Tên phòng ban", "Số lượng tổ", "Ngày tạo"};
        List<List<String>> data = list.stream().map(d -> java.util.Arrays.asList(
                d.getId().toString(),
                d.getName(),
                String.valueOf(d.getTeamCount()),
                d.getCreatedAt() != null ? d.getCreatedAt().toString() : ""
        )).collect(java.util.stream.Collectors.toList());

        byte[] bytes = excelService.exportGeneric("PhongBan", headers, data);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=danh_sach_phong_ban.xlsx")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @GetMapping("/download-template")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Tải file mẫu nhập phòng ban")
    public ResponseEntity<byte[]> downloadTemplate() throws java.io.IOException {
        String[] headers = {"Tên phòng ban"};
        byte[] bytes = excelService.getGenericTemplate("MauNhap", headers);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mau_nhap_phong_ban.xlsx")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Nhập danh sách phòng ban từ Excel")
    public ResponseEntity<ApiResponse<Void>> importExcel(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        List<DepartmentRequest> requests = excelService.importDepartments(file);
        for (DepartmentRequest req : requests) {
            departmentService.create(req);
        }
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_CREATE, null));
    }

    private final com.plywood.payroll.modules.excel.service.ExcelService excelService;
}
