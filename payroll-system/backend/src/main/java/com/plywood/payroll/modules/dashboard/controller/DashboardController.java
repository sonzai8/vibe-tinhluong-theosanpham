package com.plywood.payroll.modules.dashboard.controller;
import com.plywood.payroll.shared.dto.ApiResponse;
import com.plywood.payroll.modules.organization.repository.DepartmentRepository;
import com.plywood.payroll.modules.employee.repository.EmployeeRepository;
import com.plywood.payroll.modules.production.repository.ProductionRecordRepository;
import com.plywood.payroll.modules.organization.repository.TeamRepository;
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

        // 1. Tổng nhân sự Active (Tối ưu dùng countByStatus)
        stats.put("activeEmployees", employeeRepository.countByStatus("ACTIVE"));

        // 2. Sản lượng hôm nay (Tối ưu dùng sumQuantityByProductionDate)
        LocalDate today = LocalDate.now();
        Long todayQty = productionRecordRepository.sumQuantityByProductionDate(today);
        stats.put("todayProduction", todayQty != null ? todayQty : 0L);

        // 3. Tổng số phòng ban / Tổ đội
        stats.put("totalDepartments", departmentRepository.count());
        stats.put("totalTeams", teamRepository.count());

        // 4. Biểu đồ 7 ngày gần nhất (Dùng sumQuantityByProductionDate)
        List<Map<String, Object>> chartData = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Long qty = productionRecordRepository.sumQuantityByProductionDate(date);
            
            Map<String, Object> dayStat = new HashMap<>();
            dayStat.put("date", date.toString());
            dayStat.put("quantity", qty != null ? qty : 0L);
            chartData.add(dayStat);
        }
        stats.put("productionChart", chartData);
        
        // 5. Nhật ký sản xuất mới nhất (Tối ưu dùng findTop5ByOrderByCreatedAtDesc - FIX NPE)
        List<Map<String, Object>> latestRecords = productionRecordRepository.findTop5ByOrderByCreatedAtDesc().stream()
                .map(r -> {
                    Map<String, Object> rm = new HashMap<>();
                    if (r.getTeam() != null) {
                        rm.put("employeeName", "Team " + r.getTeam().getName());
                        rm.put("stepName", r.getTeam().getProductionStep() != null ? r.getTeam().getProductionStep().getName() : "N/A");
                    } else {
                        rm.put("employeeName", "Không xác định");
                        rm.put("stepName", "N/A");
                    }
                    rm.put("productName", r.getProduct() != null ? r.getProduct().getCode() : "N/A");
                    rm.put("quantity", r.getQuantity());
                    rm.put("status", "Hợp lệ");
                    return rm;
                })
                .collect(java.util.stream.Collectors.toList());
        stats.put("latestRecords", latestRecords);

        // 6. Top phòng ban theo số lượng nhân sự (Tối ưu dùng countByDepartment_IdAndStatus)
        List<Map<String, Object>> topDepts = departmentRepository.findAll().stream()
                .map(d -> {
                    Map<String, Object> dm = new HashMap<>();
                    dm.put("name", d.getName());
                    // Đếm nhân viên active trực thuộc phòng ban (Tối ưu qua Repository)
                    long empCount = employeeRepository.countByDepartment_IdAndStatus(d.getId(), "ACTIVE");
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
