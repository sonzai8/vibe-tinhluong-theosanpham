package com.plywood.payroll.modules.penalty.controller;

import com.plywood.payroll.modules.penalty.entity.PenaltyBonusType;
import com.plywood.payroll.modules.penalty.service.PenaltyBonusTypeService;
import com.plywood.payroll.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/penalty-bonus-types")
@RequiredArgsConstructor
public class PenaltyBonusTypeController {

    private final PenaltyBonusTypeService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PenaltyBonusType>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thành công", service.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PenaltyBonusType>> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết thành công", service.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPPER ADMIN')")
    public ResponseEntity<ApiResponse<PenaltyBonusType>> create(@RequestBody PenaltyBonusType type) {
        return ResponseEntity.ok(ApiResponse.success("Tạo thành công", service.create(type)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPPER ADMIN')")
    public ResponseEntity<ApiResponse<PenaltyBonusType>> update(@PathVariable("id") Long id, @RequestBody PenaltyBonusType type) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công", service.update(id, type)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPPER ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa loại thưởng/phạt thành công", null));
    }
}
