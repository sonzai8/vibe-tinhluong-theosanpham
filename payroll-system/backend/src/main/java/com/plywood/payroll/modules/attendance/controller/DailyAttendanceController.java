package com.plywood.payroll.modules.attendance.controller;

import com.plywood.payroll.shared.constant.MessageConstants;
import com.plywood.payroll.modules.attendance.dto.request.DailyAttendanceRequest;
import com.plywood.payroll.shared.dto.ApiResponse;
import com.plywood.payroll.modules.attendance.dto.response.DailyAttendanceResponse;
import com.plywood.payroll.modules.attendance.service.DailyAttendanceService;
import com.plywood.payroll.modules.excel.service.ExcelService;
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
    @Operation(summary = "Danh sách điểm danh theo bộ lọc")
    public ResponseEntity<ApiResponse<List<DailyAttendanceResponse>>> getByFilters(
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "departmentIds", required = false) List<Long> departmentIds,
            @RequestParam(value = "teamIds", required = false) List<Long> teamIds) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, 
                attendanceService.getByFilters(fromDate, toDate, month, year, date, departmentIds, teamIds)));
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
    public ResponseEntity<ApiResponse<DailyAttendanceResponse>> update(@PathVariable("id") Long id, @Valid @RequestBody DailyAttendanceRequest request) {
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
            @RequestParam(value = "format", defaultValue = "list") String format,
            @RequestParam(value = "departmentIds", required = false) List<Long> departmentIds,
            @RequestParam(value = "teamIds", required = false) List<Long> teamIds) throws IOException {
        
        List<DailyAttendanceResponse> data = attendanceService.getByFilters(null, null, month, year, null, departmentIds, teamIds);
        byte[] excelContent;
        String fileName;
        
        if ("matrix".equalsIgnoreCase(format)) {
            LocalDate firstDay = LocalDate.of(year, month, 1);
            int length = firstDay.lengthOfMonth();
            List<LocalDate> dates = new java.util.ArrayList<>();
            for (int i = 0; i < length; i++) {
                dates.add(firstDay.plusDays(i));
            }
            excelContent = excelService.exportAttendanceMatrix(data, dates);
            fileName = String.format("ChamCong_Matrix_%d_%d.xlsx", month, year);
        } else {
            excelContent = excelService.exportAttendances(data);
            fileName = String.format("ChamCong_DanhSach_%d_%d.xlsx", month, year);
        }
        
        ByteArrayResource resource = new ByteArrayResource(excelContent);
        
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
