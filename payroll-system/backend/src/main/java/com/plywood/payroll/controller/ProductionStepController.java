package com.plywood.payroll.controller;

import com.plywood.payroll.constant.MessageConstants;


import com.plywood.payroll.dto.request.ProductionStepRequest;
import com.plywood.payroll.dto.response.ApiResponse;
import com.plywood.payroll.dto.response.ProductionStepResponse;
import com.plywood.payroll.service.ProductionStepService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/production-steps")
@RequiredArgsConstructor
@Tag(name = "Production Step", description = "Quản lý các công đoạn sản xuất")
public class ProductionStepController {

    private final ProductionStepService productionStepService;

    @GetMapping
    @Operation(summary = "Lấy danh sách công đoạn")
    public ResponseEntity<ApiResponse<List<ProductionStepResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, productionStepService.getAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin công đoạn theo ID")
    public ResponseEntity<ApiResponse<ProductionStepResponse>> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_DETAIL, productionStepService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Tạo công đoạn sản xuất")
    public ResponseEntity<ApiResponse<ProductionStepResponse>> create(@Valid @RequestBody ProductionStepRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.SUCCESS_CREATE, productionStepService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật công đoạn")
    public ResponseEntity<ApiResponse<ProductionStepResponse>> update(@PathVariable("id") Long id, @Valid @RequestBody ProductionStepRequest request) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_UPDATE, productionStepService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa công đoạn")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        productionStepService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_DELETE, null));
    }
}
