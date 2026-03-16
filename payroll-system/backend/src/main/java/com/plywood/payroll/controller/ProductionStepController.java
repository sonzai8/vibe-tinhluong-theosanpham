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
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Xóa công đoạn")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        productionStepService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_DELETE, null));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Xuất danh sách công đoạn ra Excel")
    public ResponseEntity<byte[]> export() throws java.io.IOException {
        List<ProductionStepResponse> list = productionStepService.getAll();
        String[] headers = {"ID", "Tên công đoạn", "Mô tả", "Ngày tạo"};
        List<List<String>> data = list.stream().map(s -> java.util.Arrays.asList(
                s.getId().toString(),
                s.getName(),
                s.getDescription() != null ? s.getDescription() : "",
                s.getCreatedAt() != null ? s.getCreatedAt().toString() : ""
        )).collect(java.util.stream.Collectors.toList());

        byte[] bytes = excelService.exportGeneric("CongDoan", headers, data);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=danh_sach_cong_doan.xlsx")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @GetMapping("/download-template")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Tải file mẫu nhập công đoạn")
    public ResponseEntity<byte[]> downloadTemplate() throws java.io.IOException {
        String[] headers = {"Tên công đoạn", "Mô tả"};
        byte[] bytes = excelService.getGenericTemplate("MauNhap", headers);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mau_nhap_cong_doan.xlsx")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Nhập danh sách công đoạn từ Excel")
    public ResponseEntity<ApiResponse<Void>> importExcel(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        List<ProductionStepRequest> requests = excelService.importProductionSteps(file);
        for (ProductionStepRequest req : requests) {
            productionStepService.create(req);
        }
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_CREATE, null));
    }

    private final com.plywood.payroll.service.ExcelService excelService;
}
