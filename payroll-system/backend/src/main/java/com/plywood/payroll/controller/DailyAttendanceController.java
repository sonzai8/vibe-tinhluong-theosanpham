package com.plywood.payroll.controller;

import com.plywood.payroll.constant.MessageConstants;
import com.plywood.payroll.dto.request.DailyAttendanceRequest;
import com.plywood.payroll.dto.response.ApiResponse;
import com.plywood.payroll.dto.response.DailyAttendanceResponse;
import com.plywood.payroll.service.DailyAttendanceService;
import com.plywood.payroll.service.ExcelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendances")
@RequiredArgsConstructor
@Tag(name = "Daily Attendance", description = "Quản lý điểm danh hàng ngày")
public class DailyAttendanceController {

    private final DailyAttendanceService attendanceService;
    private final ExcelService excelService;

    @GetMapping
    @PreAuthorize("hasAuthority('ATTENDANCE_VIEW') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Danh sách điểm danh trong tháng/năm")
    public ResponseEntity<ApiResponse<List<DailyAttendanceResponse>>> getByMonthAndYear(
            @RequestParam("month") int month,
            @RequestParam("year") int year) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, attendanceService.getByMonthAndYear(month, year)));
    }
    
    @GetMapping("/date/{date}")
    @PreAuthorize("hasAuthority('ATTENDANCE_VIEW') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Danh sách điểm danh theo ngày")
    public ResponseEntity<ApiResponse<List<DailyAttendanceResponse>>> getByDate(
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, attendanceService.getByDate(date)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ATTENDANCE_VIEW') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Lấy điểm danh theo ID")
    public ResponseEntity<ApiResponse<DailyAttendanceResponse>> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_DETAIL, attendanceService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ATTENDANCE_EDIT') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Thêm một bản ghi điểm danh")
    public ResponseEntity<ApiResponse<DailyAttendanceResponse>> create(@Valid @RequestBody DailyAttendanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.SUCCESS_CREATE, attendanceService.create(request)));
    }
    
    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('ATTENDANCE_EDIT') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Thêm nhiều bản ghi điểm danh cùng lúc")
    public ResponseEntity<ApiResponse<List<DailyAttendanceResponse>>> createBatch(@RequestBody List<@Valid DailyAttendanceRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.SUCCESS_CREATE, attendanceService.saveBatch(requests)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ATTENDANCE_EDIT') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Cập nhật bản ghi điểm danh")
    public ResponseEntity<ApiResponse<DailyAttendanceResponse>> update(@PathVariable Long id, @Valid @RequestBody DailyAttendanceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_UPDATE, attendanceService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ATTENDANCE_EDIT') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Xóa điểm danh")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        attendanceService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_DELETE, null));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('ATTENDANCE_VIEW') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Xuất file Excel chấm công")
    public ResponseEntity<Resource> export(
            @RequestParam("month") int month,
            @RequestParam("year") int year,
            @RequestParam(value = "departmentId", required = false) Long departmentId) throws IOException {
        
        List<DailyAttendanceResponse> data = attendanceService.getByMonthYearAndDepartment(month, year, departmentId);
        byte[] excelContent = excelService.exportAttendances(data);
        
        ByteArrayResource resource = new ByteArrayResource(excelContent);
        String fileName = String.format("ChamCong_%d_%d.xlsx", month, year);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .contentLength(excelContent.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .body(resource);
    }

    @GetMapping("/download-template")
    @PreAuthorize("hasAuthority('ATTENDANCE_VIEW') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Tải file mẫu nhập chấm công")
    public ResponseEntity<Resource> downloadTemplate() throws IOException {
        byte[] templateContent = excelService.getImportTemplate();
        ByteArrayResource resource = new ByteArrayResource(templateContent);
        String fileName = "Mau_Nhap_Cham_Cong.xlsx";
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .contentLength(templateContent.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .body(resource);
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('ATTENDANCE_EDIT') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Nhập file Excel chấm công")
    public ResponseEntity<ApiResponse<List<DailyAttendanceResponse>>> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        List<DailyAttendanceRequest> requests = excelService.importAttendances(file);
        List<DailyAttendanceResponse> saved = attendanceService.saveBatch(requests);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_CREATE, saved));
    }
}
