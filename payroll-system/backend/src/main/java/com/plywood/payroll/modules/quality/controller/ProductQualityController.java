package com.plywood.payroll.modules.quality.controller;
import com.plywood.payroll.modules.product.entity.Product;

import com.plywood.payroll.shared.constant.MessageConstants;


import com.plywood.payroll.modules.quality.dto.request.ProductQualityRequest;
import com.plywood.payroll.shared.dto.ApiResponse;
import com.plywood.payroll.modules.quality.dto.response.ProductQualityResponse;
import com.plywood.payroll.modules.quality.service.ProductQualityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-qualities")
@RequiredArgsConstructor
@Tag(name = "Product Quality", description = "Quản lý xếp hạng chất lượng lô hàng")
public class ProductQualityController {

    private final ProductQualityService productQualityService;

    @GetMapping
    @Operation(summary = "Lấy danh sách chất lượng lô hàng")
    public ResponseEntity<ApiResponse<List<ProductQualityResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, productQualityService.getAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết và danh sách lớp lỗi của chất lượng lô hàng theo ID")
    public ResponseEntity<ApiResponse<ProductQualityResponse>> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_DETAIL, productQualityService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Tạo biên bản chất lượng kèm các lớp lỗi")
    public ResponseEntity<ApiResponse<ProductQualityResponse>> create(@Valid @RequestBody ProductQualityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.SUCCESS_CREATE, productQualityService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật biên bản chất lượng")
    public ResponseEntity<ApiResponse<ProductQualityResponse>> update(@PathVariable("id") Long id, @Valid @RequestBody ProductQualityRequest request) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_UPDATE, productQualityService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa biên bản chất lượng")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        productQualityService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_DELETE, null));
    }
}
