package com.plywood.payroll.modules.attendance.controller;

import com.plywood.payroll.modules.attendance.dto.request.AttendanceDefinitionRequest;
import com.plywood.payroll.modules.attendance.dto.response.AttendanceDefinitionResponse;
import com.plywood.payroll.modules.attendance.service.AttendanceDefinitionService;
import com.plywood.payroll.shared.constant.MessageConstants;
import com.plywood.payroll.shared.dto.ApiResponse;
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
@RequestMapping("/api/attendance-definitions")
@RequiredArgsConstructor
@Tag(name = "Attendance Definition", description = "Quản lý định nghĩa loại công")
public class AttendanceDefinitionController {

    private final AttendanceDefinitionService definitionService;

    @GetMapping
    @PreAuthorize("hasAuthority('ATTENDANCE_VIEW') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Danh sách loại công")
    public ResponseEntity<ApiResponse<List<AttendanceDefinitionResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, definitionService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ATTENDANCE_VIEW') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Chi tiết loại công")
    public ResponseEntity<ApiResponse<AttendanceDefinitionResponse>> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_DETAIL, definitionService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ATTENDANCE_EDIT') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Thêm loại công")
    public ResponseEntity<ApiResponse<AttendanceDefinitionResponse>> create(@Valid @RequestBody AttendanceDefinitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.SUCCESS_CREATE, definitionService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ATTENDANCE_EDIT') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Cập nhật loại công")
    public ResponseEntity<ApiResponse<AttendanceDefinitionResponse>> update(@PathVariable("id") Long id, @Valid @RequestBody AttendanceDefinitionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_UPDATE, definitionService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ATTENDANCE_EDIT') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Xóa loại công")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        definitionService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_DELETE, null));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('ATTENDANCE_VIEW') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Xuất danh sách loại công ra Excel")
    public ResponseEntity<byte[]> export() throws java.io.IOException {
        List<AttendanceDefinitionResponse> list = definitionService.getAll();
        String[] headers = {"ID", "Mã ký hiệu", "Tên loại công", "Hệ số", "Mô tả"};
        List<List<String>> data = list.stream().map(s -> java.util.Arrays.asList(
                s.getId().toString(),
                s.getCode(),
                s.getName(),
                s.getMultiplier().toString(),
                s.getDescription() != null ? s.getDescription() : ""
        )).collect(java.util.stream.Collectors.toList());

        byte[] bytes = excelService.exportGeneric("LoaiCong", headers, data);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=danh_sach_loai_cong.xlsx")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @GetMapping("/download-template")
    @PreAuthorize("hasAuthority('ATTENDANCE_EDIT') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Tải file mẫu nhập loại công")
    public ResponseEntity<byte[]> downloadTemplate() throws java.io.IOException {
        String[] headers = {"Mã ký hiệu", "Tên loại công", "Hệ số", "Mô tả"};
        byte[] bytes = excelService.getGenericTemplate("MauNhapLoaiCong", headers);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mau_nhap_loai_cong.xlsx")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @PostMapping("/import/preview")
    @PreAuthorize("hasAuthority('ATTENDANCE_EDIT') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Xem trước dữ liệu import loại công")
    public ResponseEntity<ApiResponse<com.plywood.payroll.modules.excel.dto.response.ImportResult<AttendanceDefinitionRequest>>> importPreview(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, excelService.importAttendanceDefinitionsPreview(file)));
    }

    @PostMapping("/import/confirm")
    @PreAuthorize("hasAuthority('ATTENDANCE_EDIT') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Xác nhận import loại công")
    public ResponseEntity<ApiResponse<Void>> importConfirm(@Valid @RequestBody List<AttendanceDefinitionRequest> requests) {
        for (AttendanceDefinitionRequest req : requests) {
            definitionService.create(req);
        }
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_CREATE, null));
    }

    private final com.plywood.payroll.modules.excel.service.ExcelService excelService;
}
