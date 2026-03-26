package com.plywood.payroll.modules.system.controller;

import com.plywood.payroll.modules.system.entity.SystemLog;
import com.plywood.payroll.modules.system.service.SystemLogService;
import com.plywood.payroll.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system-logs")
@RequiredArgsConstructor
@Tag(name = "System Logs", description = "Quản lý log hệ thống")
public class SystemLogController {

    private final SystemLogService systemLogService;

    @GetMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Lấy danh sách log hệ thống")
    public ResponseEntity<ApiResponse<Page<SystemLog>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SystemLog> logs = systemLogService.getAll(PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách log thành công", logs));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Đánh dấu log đã xử lý")
    public ResponseEntity<ApiResponse<SystemLog>> resolve(
            @PathVariable Long id,
            @RequestBody String note) {
        SystemLog resolvedLog = systemLogService.resolve(id, note);
        return ResponseEntity.ok(ApiResponse.success("Đã xử lý log", resolvedLog));
    }
}
