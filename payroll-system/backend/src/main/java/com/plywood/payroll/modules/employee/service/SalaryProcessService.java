package com.plywood.payroll.modules.employee.service;

import com.plywood.payroll.modules.employee.dto.request.SalaryProcessRequest;
import com.plywood.payroll.modules.employee.dto.response.SalaryProcessResponse;
import com.plywood.payroll.modules.employee.entity.Employee;
import com.plywood.payroll.modules.employee.entity.SalaryProcess;
import com.plywood.payroll.modules.employee.entity.TeamProcess;
import com.plywood.payroll.modules.employee.repository.EmployeeRepository;
import com.plywood.payroll.modules.employee.repository.SalaryProcessRepository;
import com.plywood.payroll.modules.employee.repository.TeamProcessRepository;
import com.plywood.payroll.shared.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalaryProcessService {

    private final SalaryProcessRepository salaryProcessRepository;
    private final EmployeeRepository employeeRepository;
    private final TeamProcessRepository teamProcessRepository;

    public Page<SalaryProcessResponse> getPaged(String searchTerm, Long teamId, Long employeeId, Pageable pageable) {
        Specification<SalaryProcess> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (employeeId != null) {
                predicates.add(cb.equal(root.get("employee").get("id"), employeeId));
            }

            if (teamId != null) {
                // Join with TeamProcess to filter by current team
                Join<SalaryProcess, Employee> employeeJoin = root.join("employee");
                // Subquery for current team
                jakarta.persistence.criteria.Subquery<Long> subquery = query.subquery(Long.class);
                jakarta.persistence.criteria.Root<TeamProcess> tpRoot = subquery.from(TeamProcess.class);
                subquery.select(tpRoot.get("employee").get("id"));
                subquery.where(
                    cb.equal(tpRoot.get("team").get("id"), teamId),
                    cb.lessThanOrEqualTo(tpRoot.get("startDate"), LocalDate.now()),
                    cb.or(cb.isNull(tpRoot.get("endDate")), cb.greaterThanOrEqualTo(tpRoot.get("endDate"), LocalDate.now()))
                );
                predicates.add(cb.in(employeeJoin.get("id")).value(subquery));
            }

            if (searchTerm != null && !searchTerm.isEmpty()) {
                String searchPattern = "%" + searchTerm.toLowerCase() + "%";
                Join<SalaryProcess, Employee> employeeJoin = root.join("employee");
                predicates.add(cb.or(
                    cb.like(cb.lower(employeeJoin.get("fullName")), searchPattern),
                    cb.like(cb.lower(employeeJoin.get("code")), searchPattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return salaryProcessRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Transactional
    public SalaryProcessResponse create(SalaryProcessRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên", request.getEmployeeId()));

        validateOverlaps(null, employee.getId(), request.getStartDate(), request.getEndDate());

        // Nếu mới là active (ngày bắt đầu <= hôm nay và kết thúc >= hôm nay)
        // thì đóng các bản ghi cũ
        LocalDate today = LocalDate.now();
        if (!request.getStartDate().isAfter(today) && (request.getEndDate() == null || !request.getEndDate().isBefore(today))) {
            deactivateOldProcesses(employee.getId());
        }

        SalaryProcess process = new SalaryProcess();
        process.setEmployee(employee);
        process.setSalaryType(request.getSalaryType());
        process.setBaseSalary(request.getBaseSalary());
        process.setInsuranceSalary(request.getInsuranceSalary());
        
        process.setStartDate(request.getStartDate() != null ? request.getStartDate() : today);
        process.setEndDate(request.getEndDate() != null ? request.getEndDate() : process.getStartDate().plusYears(1));

        return mapToResponse(salaryProcessRepository.save(process));
    }

    @Transactional
    public SalaryProcessResponse update(Long id, SalaryProcessRequest request) {
        SalaryProcess process = salaryProcessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quy trình lương", id));

        validateOverlaps(id, process.getEmployee().getId(), request.getStartDate(), request.getEndDate());

        process.setSalaryType(request.getSalaryType());
        process.setBaseSalary(request.getBaseSalary());
        process.setInsuranceSalary(request.getInsuranceSalary());
        process.setStartDate(request.getStartDate());
        process.setEndDate(request.getEndDate());

        return mapToResponse(salaryProcessRepository.save(process));
    }

    @Transactional
    public void delete(Long id) {
        if (!salaryProcessRepository.existsById(id)) {
            throw new ResourceNotFoundException("Quy trình lương", id);
        }
        salaryProcessRepository.deleteById(id);
    }

    private void validateOverlaps(Long excludeId, Long employeeId, LocalDate start, LocalDate end) {
        List<SalaryProcess> overlaps = salaryProcessRepository.findOverlapping(employeeId, start, end);
        for (SalaryProcess p : overlaps) {
            if (excludeId == null || !p.getId().equals(excludeId)) {
                throw new IllegalArgumentException("Khoảng thời gian bị trùng lặp với bản ghi ID: " + p.getId());
            }
        }
    }

    private void deactivateOldProcesses(Long employeeId) {
        LocalDate today = LocalDate.now();
        salaryProcessRepository.findEffectiveByDate(employeeId, today).ifPresent(p -> {
            // Set end date to first day of next month
            LocalDate firstDayOfNextMonth = today.plusMonths(1).withDayOfMonth(1);
            p.setEndDate(firstDayOfNextMonth);
            salaryProcessRepository.save(p);
        });
    }

    public SalaryProcessResponse mapToResponse(SalaryProcess entity) {
        SalaryProcessResponse response = new SalaryProcessResponse();
        response.setId(entity.getId());
        response.setSalaryType(entity.getSalaryType());
        response.setBaseSalary(entity.getBaseSalary());
        response.setInsuranceSalary(entity.getInsuranceSalary());
        response.setStartDate(entity.getStartDate());
        response.setEndDate(entity.getEndDate());
        
        LocalDate today = LocalDate.now();
        response.setCurrent(entity.getStartDate() != null && !entity.getStartDate().isAfter(today) && 
                           (entity.getEndDate() == null || !entity.getEndDate().isBefore(today)));
        
        if (entity.getEmployee() != null) {
            response.setEmployeeId(entity.getEmployee().getId());
            response.setEmployeeCode(entity.getEmployee().getCode());
            response.setEmployeeName(entity.getEmployee().getFullName());
            
            // Current team
            teamProcessRepository.findEffectiveByDate(entity.getEmployee().getId(), today)
                    .ifPresent(tp -> response.setTeamName(tp.getTeam().getName()));
        }
        
        return response;
    }
}
