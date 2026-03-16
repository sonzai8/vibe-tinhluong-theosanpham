package com.plywood.payroll.controller;

import com.plywood.payroll.entity.PayrollItem;
import com.plywood.payroll.entity.Payroll;
import com.plywood.payroll.service.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payrolls")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @PostMapping("/calculate")
    public Payroll calculate(@RequestBody Map<String, Integer> body) {
        int month = body.get("month");
        int year = body.get("year");
        return payrollService.calculatePayroll(month, year);
    }

    @GetMapping("/{year}/{month}")
    public List<PayrollItem> getPayroll(@PathVariable int year, @PathVariable int month) {
        return payrollService.getPayrollItems(month, year);
    }
}
