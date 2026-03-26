package com.plywood.payroll.modules.employee.controller;
import com.plywood.payroll.modules.employee.dto.request.ResetPasswordRequest;
import com.plywood.payroll.modules.employee.dto.response.EmployeeAuditLogResponse;

import com.plywood.payroll.shared.constant.MessageConstants;


import com.plywood.payroll.modules.employee.dto.request.EmployeeRequest;
import com.plywood.payroll.shared.dto.ApiResponse;
import com.plywood.payroll.modules.employee.dto.response.EmployeeResponse;
import com.plywood.payroll.modules.employee.dto.request.EmployeeNoteRequest;
import com.plywood.payroll.modules.employee.dto.response.*;
import com.plywood.payroll.modules.employee.service.EmployeeService;
import com.plywood.payroll.modules.excel.service.ExcelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Tag(name = "Employee", description = "Quản lý nhân viên")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final ExcelService excelService;

    @GetMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('EMPLOYEE_VIEW') or hasAuthority('ATTENDANCE_VIEW') or hasAuthority('PRODUCTION_VIEW') or hasAuthority('PAYROLL_VIEW')")
    @Operation(summary = "Lấy danh sách nhân viên (rút gọn/phẳng)")
    public ResponseEntity<ApiResponse<List<EmployeeListResponse>>> getAll(@RequestParam(value = "search", required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, employeeService.getAll(search)));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('EMPLOYEE_VIEW') or hasAuthority('ATTENDANCE_VIEW') or hasAuthority('PRODUCTION_VIEW') or hasAuthority('PAYROLL_VIEW')")
    @Operation(summary = "Tìm kiếm nhân viên (rút gọn)")
    public ResponseEntity<ApiResponse<List<EmployeeBasicResponse>>> searchBasic(@RequestParam("search") String search) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, employeeService.searchBasic(search)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('EMPLOYEE_VIEW') or hasAuthority('ATTENDANCE_VIEW') or hasAuthority('PRODUCTION_VIEW') or hasAuthority('PAYROLL_VIEW')")
    @Operation(summary = "Lấy thông tin nhân viên theo ID")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_DETAIL, employeeService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_EDIT') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Tạo mới nhân viên")
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(@Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.SUCCESS_CREATE, employeeService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_EDIT') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Cập nhật nhân viên")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(@PathVariable("id") Long id, @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_UPDATE, employeeService.update(id, request)));
    }

    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Đặt lại mật khẩu nhân viên")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable("id") Long id, @Valid @RequestBody ResetPasswordRequest request) {
        employeeService.resetPassword(id, request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_UPDATE, null));
    }

    @GetMapping("/{id}/audit-logs")
    @PreAuthorize("hasAuthority('EMPLOYEE_EDIT') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Xem lịch sử thay đổi của nhân viên")
    public ResponseEntity<ApiResponse<List<EmployeeAuditLogResponse>>> getAuditLogs(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, employeeService.getAuditLogs(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_EDIT') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Xóa nhân viên")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        employeeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_DELETE, null));
    }

    @GetMapping("/download-template")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Tải file mẫu nhập nhân viên")
    public ResponseEntity<byte[]> downloadTemplate() throws java.io.IOException {
        byte[] bytes = excelService.getEmployeeTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=employee_template.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @PostMapping("/import/preview")
    @PreAuthorize("hasAuthority('EMPLOYEE_EDIT') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Xem trước danh sách nhân viên từ file Excel")
    public ResponseEntity<ApiResponse<com.plywood.payroll.modules.excel.dto.response.ImportResult<EmployeeRequest>>> importPreview(@RequestParam("file") MultipartFile file) throws IOException {
        com.plywood.payroll.modules.excel.dto.response.ImportResult<EmployeeRequest> result = excelService.importEmployeesPreview(file);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, result));
    }

    @PostMapping("/import/confirm")
    @PreAuthorize("hasAuthority('EMPLOYEE_EDIT') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Xác nhận nhập danh sách nhân viên")
    public ResponseEntity<ApiResponse<Void>> importConfirm(@Valid @RequestBody List<EmployeeRequest> requests) {
        for (EmployeeRequest req : requests) {
            employeeService.create(req);
        }
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_CREATE, null));
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('EMPLOYEE_EDIT') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Nhập nhân viên từ file Excel (Trực tiếp)")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> importEmployees(@RequestParam("file") MultipartFile file) throws IOException {
        com.plywood.payroll.modules.excel.dto.response.ImportResult<EmployeeRequest> result = excelService.importEmployeesPreview(file);
        List<EmployeeResponse> responses = new java.util.ArrayList<>();
        if (result.getErrorCount() == 0) {
            responses = result.getData().stream()
                    .map(employeeService::create)
                    .toList();
        }
        return ResponseEntity.ok(ApiResponse.success("Nhập dữ liệu thành công " + responses.size() + " nhân viên", responses));
    }

    @PatchMapping("/{id}/zk-device-id")
    @PreAuthorize("hasAuthority('EMPLOYEE_EDIT') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Cập nhật nhanh ID chấm công")
    public ResponseEntity<ApiResponse<Void>> updateZkDeviceId(@PathVariable("id") Long id, @RequestBody String zkDeviceId) {
        employeeService.updateZkDeviceId(id, zkDeviceId);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_UPDATE, null));
    }

    @PostMapping("/{id}/avatar")
    @PreAuthorize("hasAuthority('EMPLOYEE_EDIT') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Tải lên ảnh đại diện nhân viên")
    public ResponseEntity<ApiResponse<String>> updateAvatar(@PathVariable("id") Long id, @RequestParam("file") MultipartFile file) throws IOException {
        String avatarUrl = employeeService.updateAvatar(id, file);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_UPDATE, avatarUrl));
    }

    @GetMapping("/{id}/notes")
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Lấy danh sách ghi chú của nhân viên")
    public ResponseEntity<ApiResponse<List<EmployeeNoteResponse>>> getNotes(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, employeeService.getNotes(id)));
    }

    @PostMapping("/{id}/notes")
    @PreAuthorize("hasAuthority('EMPLOYEE_EDIT') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Thêm ghi chú mới cho nhân viên")
    public ResponseEntity<ApiResponse<EmployeeNoteResponse>> addNote(@PathVariable("id") Long id, @Valid @RequestBody EmployeeNoteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_CREATE, employeeService.addNote(id, request)));
    }

    @DeleteMapping("/notes/{noteId}")
    @PreAuthorize("hasAuthority('EMPLOYEE_EDIT') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Xóa ghi chú")
    public ResponseEntity<ApiResponse<Void>> deleteNote(@PathVariable("noteId") Long noteId) {
        employeeService.deleteNote(noteId);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_DELETE, null));
    }

    @GetMapping("/{id}/salary-history")
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Lấy lịch sử lương của nhân viên")
    public ResponseEntity<ApiResponse<List<SalaryProcessResponse>>> getSalaryHistory(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, employeeService.getSalaryHistory(id)));
    }

    @GetMapping("/{id}/team-history")
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Lấy lịch sử tổ đội của nhân viên")
    public ResponseEntity<ApiResponse<List<TeamProcessResponse>>> getTeamHistory(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, employeeService.getTeamHistory(id)));
    }
}
