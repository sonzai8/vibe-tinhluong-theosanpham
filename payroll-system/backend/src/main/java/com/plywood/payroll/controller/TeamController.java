package com.plywood.payroll.controller;

import com.plywood.payroll.constant.MessageConstants;


import com.plywood.payroll.dto.request.TeamMemberRequest;
import com.plywood.payroll.dto.request.TeamRequest;
import com.plywood.payroll.dto.response.ApiResponse;
import com.plywood.payroll.dto.response.TeamMemberResponse;
import com.plywood.payroll.dto.response.TeamResponse;
import com.plywood.payroll.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@Tag(name = "Team", description = "Quản lý tổ sản xuất và thành viên")
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    @Operation(summary = "Lấy danh sách tổ sản xuất")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, teamService.getAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin tổ theo ID")
    public ResponseEntity<ApiResponse<TeamResponse>> getById(@PathVariable Long id) {
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
    public ResponseEntity<ApiResponse<TeamResponse>> update(@PathVariable Long id, @Valid @RequestBody TeamRequest request) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_UPDATE, teamService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa tổ sản xuất")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        teamService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_DELETE, null));
    }

    // --- Member Management ---

    @GetMapping("/{id}/members")
    @Operation(summary = "Danh sách nhân viên trong tổ")
    public ResponseEntity<ApiResponse<List<TeamMemberResponse>>> getMembers(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, teamService.getMembers(id)));
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Thêm nhân viên vào biên chế tổ")
    public ResponseEntity<ApiResponse<TeamMemberResponse>> addMember(
            @PathVariable Long id, 
            @Valid @RequestBody TeamMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.SUCCESS_CREATE, teamService.addMember(id, request)));
    }

    @DeleteMapping("/{id}/members/{employeeId}")
    @Operation(summary = "Xóa nhân viên khỏi biên chế tổ")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long id, 
            @PathVariable Long employeeId) {
        teamService.removeMember(id, employeeId);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_DELETE, null));
    }
}
