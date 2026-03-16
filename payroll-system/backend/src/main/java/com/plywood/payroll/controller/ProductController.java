package com.plywood.payroll.controller;

import com.plywood.payroll.constant.MessageConstants;


import com.plywood.payroll.dto.request.ProductRequest;
import com.plywood.payroll.dto.response.ApiResponse;
import com.plywood.payroll.dto.response.ProductResponse;
import com.plywood.payroll.service.ProductService;
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
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product", description = "Quản lý sản phẩm ván ép")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Lấy danh sách sản phẩm")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, productService.getAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin sản phẩm theo ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_DETAIL, productService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Tạo mới sản phẩm")
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.SUCCESS_CREATE, productService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật sản phẩm")
    public ResponseEntity<ApiResponse<ProductResponse>> update(@PathVariable("id") Long id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_UPDATE, productService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCTION_EDIT') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Xóa sản phẩm")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_DELETE, null));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('PRODUCTION_VIEW') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Xuất danh sách sản phẩm ra Excel")
    public ResponseEntity<byte[]> export() throws java.io.IOException {
        List<ProductResponse> list = productService.getAll();
        String[] headers = {"ID", "Mã SP", "Độ dày (ly)", "Dài (m)", "Rộng (m)"};
        List<List<String>> data = list.stream().map(p -> java.util.Arrays.asList(
                p.getId().toString(),
                p.getCode(),
                p.getThickness().toString(),
                p.getLength().toString(),
                p.getWidth().toString()
        )).collect(java.util.stream.Collectors.toList());

        byte[] bytes = excelService.exportGeneric("SanPham", headers, data);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=danh_sach_san_pham.xlsx")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @GetMapping("/download-template")
    @PreAuthorize("hasAuthority('PRODUCTION_EDIT') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Tải file mẫu nhập sản phẩm")
    public ResponseEntity<byte[]> downloadTemplate() throws java.io.IOException {
        String[] headers = {"Mã SP", "Độ dày (ly)", "Dài (m)", "Rộng (m)"};
        byte[] bytes = excelService.getGenericTemplate("MauNhap", headers);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mau_nhap_san_pham.xlsx")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('PRODUCTION_EDIT') or hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Nhập danh sách sản phẩm từ Excel")
    public ResponseEntity<ApiResponse<Void>> importExcel(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        List<ProductRequest> requests = excelService.importProducts(file);
        for (ProductRequest req : requests) {
            productService.create(req);
        }
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_CREATE, null));
    }

    private final com.plywood.payroll.service.ExcelService excelService;
}
