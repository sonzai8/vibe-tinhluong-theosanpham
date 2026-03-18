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
}
