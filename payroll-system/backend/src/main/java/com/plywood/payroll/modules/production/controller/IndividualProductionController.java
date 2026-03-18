package com.plywood.payroll.modules.production.controller;

import com.plywood.payroll.modules.production.dto.response.IndividualProductionResponse;
import com.plywood.payroll.modules.production.service.IndividualProductionService;
import com.plywood.payroll.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/individual-productions")
@RequiredArgsConstructor
@Tag(name = "Individual Production", description = "Theo dõi sản lượng cá nhân chia đầu người")
public class IndividualProductionController {

    private final IndividualProductionService individualProductionService;

    @GetMapping
    @Operation(summary = "Lấy danh sách sản lượng cá nhân theo bộ lọc")
    public ResponseEntity<ApiResponse<List<IndividualProductionResponse>>> getIndividualProductions(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "departmentIds", required = false) List<Long> departmentIds,
            @RequestParam(value = "teamIds", required = false) List<Long> teamIds) {
        
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy dữ liệu thành công",
                individualProductionService.getIndividualProductions(from, to, departmentIds, teamIds)
        ));
    }
}
