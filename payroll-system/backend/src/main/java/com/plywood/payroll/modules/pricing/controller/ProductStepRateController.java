package com.plywood.payroll.modules.pricing.controller;
import com.plywood.payroll.modules.product.entity.Product;

import com.plywood.payroll.shared.constant.MessageConstants;


import com.plywood.payroll.modules.pricing.dto.request.ProductStepRateRequest;
import com.plywood.payroll.shared.dto.ApiResponse;
import com.plywood.payroll.modules.pricing.dto.response.ProductStepRateResponse;
import com.plywood.payroll.modules.pricing.service.ProductStepRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-step-rates")
@RequiredArgsConstructor
@Tag(name = "Product Step Rate", description = "Quản lý đơn giá sản phẩm theo từng công đoạn")
public class ProductStepRateController {

    private final ProductStepRateService rateService;

    @GetMapping("/export")
    @Operation(summary = "Xuất danh sách đơn giá ra Excel")
    public void export(@RequestParam(name = "type", defaultValue = "list") String type, jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fileName = "list".equals(type) ? "don_gia_danh_sach.xlsx" : "don_gia_ma_tran.xlsx";
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        
        byte[] bytes = rateService.exportExcel(type);
        response.getOutputStream().write(bytes);
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách các đơn giá")
    public ResponseEntity<ApiResponse<List<ProductStepRateResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, rateService.getAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin đơn giá theo ID")
    public ResponseEntity<ApiResponse<ProductStepRateResponse>> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_DETAIL, rateService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Thiết lập đơn giá mới")
    public ResponseEntity<ApiResponse<ProductStepRateResponse>> create(@Valid @RequestBody ProductStepRateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.SUCCESS_CREATE, rateService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật đơn giá")
    public ResponseEntity<ApiResponse<ProductStepRateResponse>> update(@PathVariable("id") Long id, @Valid @RequestBody ProductStepRateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_UPDATE, rateService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa đơn giá")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        rateService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_DELETE, null));
    }
}
