package com.plywood.payroll.modules.production.service;

import com.plywood.payroll.modules.attendance.entity.DailyAttendance;
import com.plywood.payroll.modules.attendance.repository.DailyAttendanceRepository;
import com.plywood.payroll.modules.production.dto.response.IndividualProductionResponse;
import com.plywood.payroll.modules.production.entity.ProductionRecord;
import com.plywood.payroll.modules.production.repository.ProductionRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IndividualProductionService {

    private final ProductionRecordRepository recordRepository;
    private final DailyAttendanceRepository attendanceRepository;

    public List<IndividualProductionResponse> getIndividualProductions(
            LocalDate from, LocalDate to, List<Long> departmentIds, List<Long> teamIds) {
        
        // 1. Lấy tất cả bản ghi sản lượng trong khoảng thời gian
        Specification<ProductionRecord> recordSpec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("productionDate"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("productionDate"), to));
            if (departmentIds != null && !departmentIds.isEmpty()) {
                predicates.add(root.get("team").get("department").get("id").in(departmentIds));
            }
            if (teamIds != null && !teamIds.isEmpty()) {
                predicates.add(root.get("team").get("id").in(teamIds));
            }
            if (query != null) {
                query.orderBy(cb.desc(root.get("productionDate")), cb.desc(root.get("createdAt")));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        
        List<ProductionRecord> records = recordRepository.findAll(recordSpec);
        
        // 2. Nhóm sản lượng theo ngày và tổ: Map<Date, Map<TeamId, TotalQuantity>>
        Map<LocalDate, Map<Long, Integer>> teamQtyMap = records.stream()
                .collect(Collectors.groupingBy(
                        ProductionRecord::getProductionDate,
                        Collectors.groupingBy(
                                r -> r.getTeam().getId(),
                                Collectors.summingInt(ProductionRecord::getQuantity)
                        )
                ));

        // 3. Lấy tất cả bản ghi chấm công trong khoảng thời gian
        // Lưu ý: filter theo actualTeam (tổ thực tế làm việc)
        Specification<DailyAttendance> attendanceSpec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("attendanceDate"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("attendanceDate"), to));
            if (departmentIds != null && !departmentIds.isEmpty()) {
                predicates.add(root.get("employee").get("department").get("id").in(departmentIds));
            }
            if (teamIds != null && !teamIds.isEmpty()) {
                predicates.add(root.get("actualTeam").get("id").in(teamIds));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        List<DailyAttendance> attendances = attendanceRepository.findAll(attendanceSpec);

        // 4. Đếm số người đi làm theo ngày và tổ thực tế: Map<Date, Map<TeamId, Count>>
        Map<LocalDate, Map<Long, Long>> teamAttendanceCountMap = attendances.stream()
                .collect(Collectors.groupingBy(
                        DailyAttendance::getAttendanceDate,
                        Collectors.groupingBy(
                                a -> a.getActualTeam().getId(),
                                Collectors.counting()
                        )
                ));

        // 5. Tính toán sản lượng cho từng người
        List<IndividualProductionResponse> results = new ArrayList<>();
        for (DailyAttendance att : attendances) {
            LocalDate date = att.getAttendanceDate();
            Long actualTeamId = att.getActualTeam().getId();
            
            Double totalQty = 0.0;
            if (teamQtyMap.containsKey(date) && teamQtyMap.get(date).containsKey(actualTeamId)) {
                totalQty = teamQtyMap.get(date).get(actualTeamId).doubleValue();
            }
            
            Long workerCount = 1L;
            if (teamAttendanceCountMap.containsKey(date) && teamAttendanceCountMap.get(date).containsKey(actualTeamId)) {
                workerCount = teamAttendanceCountMap.get(date).get(actualTeamId);
            }
            
            Double indQty = workerCount > 0 ? totalQty / workerCount : 0.0;
            
            results.add(IndividualProductionResponse.builder()
                    .employeeId(att.getEmployee().getId())
                    .employeeName(att.getEmployee().getFullName())
                    .employeeCode(att.getEmployee().getCode())
                    .date(date)
                    .actualTeamId(actualTeamId)
                    .actualTeamName(att.getActualTeam().getName())
                    .quantity(Math.round(indQty * 100.0) / 100.0) // Làm tròn 2 chữ số thập phân
                    .build());
        }

        return results;
    }
}
