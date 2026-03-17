package com.plywood.payroll.modules.product.controller;

import com.plywood.payroll.modules.product.dto.request.ProductUnitRequest;
import com.plywood.payroll.modules.product.dto.response.ProductUnitResponse;
import com.plywood.payroll.shared.constant.MessageConstants;
import com.plywood.payroll.shared.dto.ApiResponse;
import com.plywood.payroll.modules.product.service.ProductUnitService;
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
@RequestMapping("/api/product-units")
@RequiredArgsConstructor
@Tag(name = "Product Unit", description = "Quản lý đơn vị tính sản phẩm")
public class ProductUnitController {

    private final ProductUnitService productUnitService;

    @GetMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('CATEGORIES_VIEW')")
    @Operation(summary = "Lấy danh sách đơn vị sản phẩm")
    public ResponseEntity<ApiResponse<List<ProductUnitResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, productUnitService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('CATEGORIES_VIEW')")
    @Operation(summary = "Lấy chi tiết đơn vị sản phẩm")
    public ResponseEntity<ApiResponse<ProductUnitResponse>> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_DETAIL, productUnitService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('CATEGORIES_EDIT')")
    @Operation(summary = "Tạo mới đơn vị sản phẩm")
    public ResponseEntity<ApiResponse<ProductUnitResponse>> create(@Valid @RequestBody ProductUnitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.SUCCESS_CREATE, productUnitService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('CATEGORIES_EDIT')")
    @Operation(summary = "Cập nhật đơn vị sản phẩm")
    public ResponseEntity<ApiResponse<ProductUnitResponse>> update(@PathVariable("id") Long id, @Valid @RequestBody ProductUnitRequest request) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_UPDATE, productUnitService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('CATEGORIES_EDIT')")
    @Operation(summary = "Xóa đơn vị sản phẩm")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        productUnitService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_DELETE, null));
    }
}
