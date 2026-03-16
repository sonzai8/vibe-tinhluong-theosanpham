package com.plywood.payroll.controller;

import com.plywood.payroll.constant.MessageConstants;


import com.plywood.payroll.dto.request.QualityLayerSurchargeRequest;
import com.plywood.payroll.dto.response.ApiResponse;
import com.plywood.payroll.dto.response.QualityLayerSurchargeResponse;
import com.plywood.payroll.service.QualityLayerSurchargeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quality-layer-surcharges")
@RequiredArgsConstructor
@Tag(name = "Quality Layer Surcharge", description = "Quản lý mức phạt lỗi của từng lớp ván")
public class QualityLayerSurchargeController {

    private final QualityLayerSurchargeService surchargeService;

    @GetMapping
    @Operation(summary = "Lấy danh sách các mức phạt")
    public ResponseEntity<ApiResponse<List<QualityLayerSurchargeResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, surchargeService.getAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin mức phạt theo ID")
    public ResponseEntity<ApiResponse<QualityLayerSurchargeResponse>> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_DETAIL, surchargeService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Tạo mức phạt mới")
    public ResponseEntity<ApiResponse<QualityLayerSurchargeResponse>> create(@Valid @RequestBody QualityLayerSurchargeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.SUCCESS_CREATE, surchargeService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật mức phạt")
    public ResponseEntity<ApiResponse<QualityLayerSurchargeResponse>> update(@PathVariable("id") Long id, @Valid @RequestBody QualityLayerSurchargeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_UPDATE, surchargeService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa mức phạt")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        surchargeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_DELETE, null));
    }
}
