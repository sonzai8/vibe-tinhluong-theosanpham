package com.plywood.payroll.modules.organization.controller;
import com.plywood.payroll.modules.excel.service.ExcelService;
import com.plywood.payroll.modules.organization.entity.Team;

import com.plywood.payroll.shared.constant.MessageConstants;


import com.plywood.payroll.modules.organization.dto.request.TeamMemberRequest;
import com.plywood.payroll.modules.organization.dto.request.TeamRequest;
import com.plywood.payroll.shared.dto.ApiResponse;
import com.plywood.payroll.modules.organization.dto.response.TeamMemberResponse;
import com.plywood.payroll.modules.organization.dto.response.TeamResponse;
import com.plywood.payroll.modules.organization.service.TeamService;
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
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@Tag(name = "Team", description = "Quản lý tổ sản xuất và thành viên")
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('ATTENDANCE_VIEW') or hasAuthority('EMPLOYEE_VIEW') or hasAuthority('PRODUCTION_VIEW')")
    @Operation(summary = "Lấy danh sách tổ sản xuất")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, teamService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('ATTENDANCE_VIEW') or hasAuthority('EMPLOYEE_VIEW') or hasAuthority('PRODUCTION_VIEW')")
    @Operation(summary = "Lấy thông tin tổ theo ID")
    public ResponseEntity<ApiResponse<TeamResponse>> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_DETAIL, teamService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Tạo tổ sản xuất mới")
    public ResponseEntity<ApiResponse<TeamResponse>> create(@Valid @RequestBody TeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.SUCCESS_CREATE, teamService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật tổ sản xuất")
    public ResponseEntity<ApiResponse<TeamResponse>> update(@PathVariable("id") Long id, @Valid @RequestBody TeamRequest request) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_UPDATE, teamService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa tổ sản xuất")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        teamService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_DELETE, null));
    }

    // --- Member Management ---

    @GetMapping("/{id}/members")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('ATTENDANCE_VIEW') or hasAuthority('EMPLOYEE_VIEW') or hasAuthority('PRODUCTION_VIEW')")
    @Operation(summary = "Danh sách nhân viên trong tổ")
    public ResponseEntity<ApiResponse<List<TeamMemberResponse>>> getMembers(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, teamService.getMembers(id)));
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Thêm nhân viên vào biên chế tổ")
    public ResponseEntity<ApiResponse<TeamMemberResponse>> addMember(
            @PathVariable("id") Long id, 
            @Valid @RequestBody TeamMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.SUCCESS_CREATE, teamService.addMember(id, request)));
    }

    @DeleteMapping("/{id}/members/{employeeId}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Xóa nhân viên khỏi biên chế tổ")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable("id") Long id, 
            @PathVariable("employeeId") Long employeeId) {
        teamService.removeMember(id, employeeId);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_DELETE, null));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Xuất danh sách tổ sản xuất ra Excel")
    public ResponseEntity<byte[]> export() throws java.io.IOException {
        List<TeamResponse> list = teamService.getAll();
        String[] headers = {"ID", "Tên tổ đội", "Phòng ban", "Số thành viên", "Ngày tạo"};
        List<List<String>> data = list.stream().map(t -> java.util.Arrays.asList(
                t.getId().toString(),
                t.getName(),
                t.getDepartment() != null ? t.getDepartment().getName() : "",
                String.valueOf(t.getMemberCount()),
                t.getCreatedAt() != null ? t.getCreatedAt().toString() : ""
        )).collect(java.util.stream.Collectors.toList());

        byte[] bytes = excelService.exportGeneric("ToDoi", headers, data);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=danh_sach_to_doi.xlsx")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @GetMapping("/download-template")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Tải file mẫu nhập tổ đội")
    public ResponseEntity<byte[]> downloadTemplate() throws java.io.IOException {
        String[] headers = {"Tên tổ đội", "Tên phòng ban"};
        byte[] bytes = excelService.getGenericTemplate("MauNhap", headers);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mau_nhap_to_doi.xlsx")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Nhập danh sách tổ đội từ Excel")
    public ResponseEntity<ApiResponse<Void>> importExcel(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        List<TeamRequest> requests = excelService.importTeams(file);
        for (TeamRequest req : requests) {
            teamService.create(req);
        }
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_CREATE, null));
    }

    private final com.plywood.payroll.modules.excel.service.ExcelService excelService;
}
