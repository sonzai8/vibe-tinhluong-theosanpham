package com.plywood.payroll.controller;

import com.plywood.payroll.dto.response.ApiResponse;
import com.plywood.payroll.repository.DepartmentRepository;
import com.plywood.payroll.repository.EmployeeRepository;
import com.plywood.payroll.repository.ProductionRecordRepository;
import com.plywood.payroll.repository.TeamRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "API Thống kê trang chủ")
public class DashboardController {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final ProductionRecordRepository productionRecordRepository;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 1. Tổng nhân sự Active
        long activeEmployees = employeeRepository.findAll().stream()
                .filter(e -> "ACTIVE".equals(e.getStatus()))
                .count();
        stats.put("activeEmployees", activeEmployees);

        // 2. Sản lượng hôm nay
        LocalDate today = LocalDate.now();
        int todayProduction = productionRecordRepository.findAll().stream()
                .filter(r -> r.getProductionDate().equals(today))
                .mapToInt(r -> r.getQuantity() != null ? r.getQuantity() : 0)
                .sum();
        stats.put("todayProduction", todayProduction);

        // 3. Tổng số phòng ban / Tổ đội
        stats.put("totalDepartments", departmentRepository.count());
        stats.put("totalTeams", teamRepository.count());

        // 4. Biểu đồ 7 ngày gần nhất
        List<Map<String, Object>> chartData = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            int qty = productionRecordRepository.findAll().stream()
                    .filter(r -> r.getProductionDate().equals(date))
                    .mapToInt(r -> r.getQuantity() != null ? r.getQuantity() : 0)
                    .sum();
            
            Map<String, Object> dayStat = new HashMap<>();
            dayStat.put("date", date.toString());
            dayStat.put("quantity", qty);
            chartData.add(dayStat);
        }
        stats.put("productionChart", chartData);
        
        // 5. Nhật ký sản xuất mới nhất (Top 5)
        List<Map<String, Object>> latestRecords = productionRecordRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(com.plywood.payroll.entity.ProductionRecord::getCreatedAt).reversed())
                .limit(5)
                .map(r -> {
                    Map<String, Object> rm = new HashMap<>();
                    rm.put("employeeName", r.getTeam() != null ? "Team " + r.getTeam().getName() : "Không xác định");
                    rm.put("productName", r.getProduct() != null ? r.getProduct().getCode() : "N/A");
                    rm.put("stepName", r.getTeam() != null && r.getTeam().getProductionStep() != null ? r.getTeam().getProductionStep().getName() : "N/A");
                    rm.put("quantity", r.getQuantity());
                    rm.put("status", "Hợp lệ");
                    return rm;
                })
                .collect(java.util.stream.Collectors.toList());
        stats.put("latestRecords", latestRecords);

        // 6. Top phòng ban theo số lượng nhân sự
        List<Map<String, Object>> topDepts = departmentRepository.findAll().stream()
                .map(d -> {
                    Map<String, Object> dm = new HashMap<>();
                    dm.put("name", d.getName());
                    long empCount = employeeRepository.findAll().stream()
                            .filter(e -> d.equals(e.getDepartment()) && "ACTIVE".equals(e.getStatus()))
                            .count();
                    dm.put("employees", empCount);
                    return dm;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("employees"), (Long) a.get("employees")))
                .limit(4)
                .collect(java.util.stream.Collectors.toList());
        stats.put("topDepartments", topDepts);

        return ResponseEntity.ok(ApiResponse.success("Lấy dữ liệu thống kê thành công", stats));
    }
}
