package com.plywood.payroll.controller;

import com.plywood.payroll.entity.ProductionRecord;
import com.plywood.payroll.repository.ProductionRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/production-records")
@RequiredArgsConstructor
public class ProductionRecordController {

    private final ProductionRecordRepository productionRecordRepository;

    @GetMapping
    public List<ProductionRecord> getAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        if (from != null && to != null) {
            return productionRecordRepository.findByProductionDateBetween(from, to);
        }
        return productionRecordRepository.findAll();
    }

    @PostMapping
    public ProductionRecord create(@RequestBody ProductionRecord record) {
        return productionRecordRepository.save(record);
    }
}
