package com.plywood.payroll.modules.penalty.service;
import com.plywood.payroll.modules.employee.service.EmployeeService;

import com.plywood.payroll.modules.penalty.dto.request.PenaltyBonusRequest;
import com.plywood.payroll.modules.penalty.dto.response.PenaltyBonusResponse;
import com.plywood.payroll.modules.penalty.dto.response.PenaltyBonusSummaryResponse;
import com.plywood.payroll.modules.employee.entity.Employee;
import com.plywood.payroll.modules.penalty.entity.PenaltyBonus;
import com.plywood.payroll.modules.penalty.entity.PenaltyBonusType;
import com.plywood.payroll.shared.exception.ResourceNotFoundException;
import com.plywood.payroll.modules.employee.repository.EmployeeRepository;
import com.plywood.payroll.modules.penalty.repository.PenaltyBonusRepository;
import com.plywood.payroll.modules.penalty.repository.PenaltyBonusTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PenaltyBonusService {

    private final PenaltyBonusRepository penaltyBonusRepository;
    private final PenaltyBonusTypeRepository penaltyBonusTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;

    public List<PenaltyBonusResponse> getAll() {
        return penaltyBonusRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<PenaltyBonusResponse> getByEmployee(Long employeeId) {
        return penaltyBonusRepository.findByEmployeeId(employeeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PenaltyBonusResponse create(PenaltyBonusRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên", request.getEmployeeId()));

        PenaltyBonus pb = new PenaltyBonus();
        pb.setEmployee(employee);
        pb.setRecordDate(request.getRecordDate());
        pb.setAmount(request.getAmount());
        pb.setReason(request.getReason());

        if (request.getTypeId() != null) {
            PenaltyBonusType type = penaltyBonusTypeRepository.findById(request.getTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Loại thưởng/phạt", request.getTypeId()));
            pb.setType(type);
        }

        return mapToResponse(penaltyBonusRepository.save(pb));
    }

    @Transactional
    public PenaltyBonusResponse update(Long id, PenaltyBonusRequest request) {
        PenaltyBonus pb = penaltyBonusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khen thưởng/Kỷ luật", id));

        pb.setRecordDate(request.getRecordDate());
        pb.setAmount(request.getAmount());
        pb.setReason(request.getReason());

        if (request.getTypeId() != null) {
            PenaltyBonusType type = penaltyBonusTypeRepository.findById(request.getTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Loại thưởng/phạt", request.getTypeId()));
            pb.setType(type);
        } else {
            pb.setType(null);
        }

        return mapToResponse(penaltyBonusRepository.save(pb));
    }

    @Transactional
    public void delete(Long id) {
        if (!penaltyBonusRepository.existsById(id)) {
            throw new ResourceNotFoundException("Khen thưởng/Kỷ luật", id);
        }
        penaltyBonusRepository.deleteById(id);
    }

    public PenaltyBonusResponse mapToResponse(PenaltyBonus entity) {
        if (entity == null) return null;
        PenaltyBonusResponse response = new PenaltyBonusResponse();
        response.setId(entity.getId());
        response.setEmployee(employeeService.mapToResponse(entity.getEmployee()));
        response.setRecordDate(entity.getRecordDate());
        response.setType(entity.getType());
        response.setAmount(entity.getAmount());
        response.setReason(entity.getReason());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    public List<PenaltyBonusSummaryResponse> getSummary(LocalDate startDate, LocalDate endDate, Long departmentId, Long teamId) {
        List<PenaltyBonus> records = penaltyBonusRepository.findByFilters(startDate, endDate, departmentId, teamId);

        Map<Employee, List<PenaltyBonus>> groupedTestByEmployee = records.stream()
                .collect(Collectors.groupingBy(PenaltyBonus::getEmployee));

        List<PenaltyBonusSummaryResponse> summary = new ArrayList<>();
        for (Map.Entry<Employee, List<PenaltyBonus>> entry : groupedTestByEmployee.entrySet()) {
            Employee emp = entry.getKey();
            List<PenaltyBonus> empRecords = entry.getValue();

            BigDecimal totalPenalty = empRecords.stream()
                    .filter(r -> r.getAmount().compareTo(BigDecimal.ZERO) < 0)
                    .map(PenaltyBonus::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalBonus = empRecords.stream()
                    .filter(r -> r.getAmount().compareTo(BigDecimal.ZERO) > 0)
                    .map(PenaltyBonus::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long penaltyCount = empRecords.stream()
                    .filter(r -> r.getAmount().compareTo(BigDecimal.ZERO) < 0)
                    .count();

            long bonusCount = empRecords.stream()
                    .filter(r -> r.getAmount().compareTo(BigDecimal.ZERO) > 0)
                    .count();

            summary.add(PenaltyBonusSummaryResponse.builder()
                    .employeeId(emp.getId())
                    .employeeCode(emp.getCode())
                    .employeeName(emp.getFullName())
                    .departmentName(emp.getDepartment() != null ? emp.getDepartment().getName() : (emp.getTeam() != null && emp.getTeam().getDepartment() != null ? emp.getTeam().getDepartment().getName() : "N/A"))
                    .teamName(emp.getTeam() != null ? emp.getTeam().getName() : "N/A")
                    .totalPenalty(totalPenalty)
                    .totalBonus(totalBonus)
                    .netAmount(totalBonus.add(totalPenalty))
                    .penaltyCount(penaltyCount)
                    .bonusCount(bonusCount)
                    .build());
        }
        return summary;
    }

    public Map<String, Object> getEmployeeStats(Long employeeId) {
        List<PenaltyBonus> records = penaltyBonusRepository.findByEmployeeId(employeeId);

        BigDecimal totalPenalty = records.stream()
                .filter(r -> r.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .map(PenaltyBonus::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBonus = records.stream()
                .filter(r -> r.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .map(PenaltyBonus::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long penaltyCount = records.stream()
                .filter(r -> r.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .count();

        long bonusCount = records.stream()
                .filter(r -> r.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .count();

        return Map.of(
                "totalPenalty", totalPenalty,
                "totalBonus", totalBonus,
                "netAmount", totalBonus.add(totalPenalty),
                "penaltyCount", penaltyCount,
                "bonusCount", bonusCount,
                "history", records.stream().map(this::mapToResponse).collect(Collectors.toList())
        );
    }
}
