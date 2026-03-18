package com.plywood.payroll.modules.payroll.controller;
import com.plywood.payroll.modules.payroll.entity.Payroll;

import com.plywood.payroll.shared.constant.MessageConstants;
import com.plywood.payroll.shared.dto.ApiResponse;
import com.plywood.payroll.modules.payroll.entity.PayrollConfig;
import com.plywood.payroll.modules.payroll.service.PayrollConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payroll-configs")
@RequiredArgsConstructor
@Tag(name = "Payroll Config", description = "Quản lý cấu hình tính lương")
public class PayrollConfigController {

    private final PayrollConfigService configService;

    @GetMapping
    @Operation(summary = "Lấy danh sách cấu hình")
    public ResponseEntity<ApiResponse<List<PayrollConfig>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_LIST, configService.getAll()));
    }

    @GetMapping("/{key}")
    @Operation(summary = "Lấy giá trị cấu hình hiệu lực")
    public ResponseEntity<ApiResponse<String>> getEffectiveValue(
            @PathVariable("key") String key,
            @RequestParam("month") Integer month,
            @RequestParam("year") Integer year) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_GET_DETAIL, configService.getEffectiveValue(key, month, year)));
    }

    @PutMapping("/{key}")
    @Operation(summary = "Cập nhật giá trị cấu hình")
    public ResponseEntity<ApiResponse<PayrollConfig>> update(
            @PathVariable("key") String key,
            @RequestBody Map<String, Object> body) {
        String value = body.get("value").toString();
        Integer month = body.containsKey("month") ? Integer.parseInt(body.get("month").toString()) : null;
        Integer year = body.containsKey("year") ? Integer.parseInt(body.get("year").toString()) : null;
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS_UPDATE, configService.update(key, value, month, year)));
    }
}
