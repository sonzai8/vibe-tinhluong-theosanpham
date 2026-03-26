package com.plywood.payroll.modules.employee.controller;

import com.plywood.payroll.modules.employee.dto.request.SalaryProcessRequest;
import com.plywood.payroll.modules.employee.dto.response.SalaryProcessResponse;
import com.plywood.payroll.modules.employee.service.SalaryProcessService;
import com.plywood.payroll.shared.constant.MessageConstants;
import com.plywood.payroll.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/salary-processes")
@RequiredArgsConstructor
@Tag(name = "Salary Process", description = "Quản lý quy trình/lịch sử lương")
public class SalaryProcessController {

    private final SalaryProcessService salaryProcessService;

    @GetMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('EMPLOYEE_VIEW')")
    @Operation(summary = "Lấy danh sách quy trình lương (phân trang & lọc)")
    public ResponseEntity<ApiResponse<Page<SalaryProcessResponse>>> getPaged(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "teamId", required = false) Long teamId,
            @RequestParam(value = "employeeId", required = false) Long employeeId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sortBy", defaultValue = "startDate") String sortBy,
            @RequestParam(value = "direction", defaultValue = "DESC") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, 
                salaryProcessService.getPaged(search, teamId, employeeId, pageable)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('EMPLOYEE_EDIT')")
    @Operation(summary = "Tạo mới quy trình lương")
    public ResponseEntity<ApiResponse<SalaryProcessResponse>> create(@Valid @RequestBody SalaryProcessRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.SUCCESS_CREATE, salaryProcessService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('EMPLOYEE_EDIT')")
    @Operation(summary = "Cập nhật quy trình lương (Chỉnh sửa nhanh)")
    public ResponseEntity<ApiResponse<SalaryProcessResponse>> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody SalaryProcessRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_UPDATE, salaryProcessService.update(id, request)));
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('EMPLOYEE_EDIT')")
    @Operation(summary = "Xóa quy trình lương")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        salaryProcessService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_DELETE, null));
    }
}
