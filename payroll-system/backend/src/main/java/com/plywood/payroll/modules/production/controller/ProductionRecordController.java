package com.plywood.payroll.modules.production.controller;

import com.plywood.payroll.shared.constant.MessageConstants;


import com.plywood.payroll.modules.production.dto.request.ProductionRecordRequest;
import com.plywood.payroll.shared.dto.ApiResponse;
import com.plywood.payroll.modules.production.dto.response.ProductionRecordResponse;
import com.plywood.payroll.modules.production.service.ProductionRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/production-records")
@RequiredArgsConstructor
@Tag(name = "Production Record", description = "Quản lý ghi nhận sản lượng sản xuất")
public class ProductionRecordController {

    private final ProductionRecordService recordService;

    @GetMapping
    @Operation(summary = "Lấy danh sách bản ghi theo bộ lọc linh hoạt")
    public ResponseEntity<ApiResponse<List<ProductionRecordResponse>>> getByFilters(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "departmentIds", required = false) List<Long> departmentIds,
            @RequestParam(value = "teamIds", required = false) List<Long> teamIds) {
        return ResponseEntity.ok(ApiResponse.success(
                MessageConstants.SUCCESS_GET_LIST, 
                recordService.getByFilters(from, to, departmentIds, teamIds)
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết bản ghi sản xuất theo ID")
    public ResponseEntity<ApiResponse<ProductionRecordResponse>> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_DETAIL, recordService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Thêm bản ghi sản lượng mới")
    public ResponseEntity<ApiResponse<ProductionRecordResponse>> create(@Valid @RequestBody ProductionRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.SUCCESS_CREATE, recordService.create(request)));
    }
    
    @PostMapping("/batch")
    @Operation(summary = "Thêm nhiều bản ghi sản lượng cùng lúc")
    public ResponseEntity<ApiResponse<List<ProductionRecordResponse>>> createBatch(@RequestBody List<@Valid ProductionRecordRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.SUCCESS_CREATE, recordService.saveBatch(requests)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật bản ghi sản lượng")
    public ResponseEntity<ApiResponse<ProductionRecordResponse>> update(@PathVariable("id") Long id, @Valid @RequestBody ProductionRecordRequest request) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_UPDATE, recordService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa bản ghi sản lượng")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        recordService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_DELETE, null));
    }
}
