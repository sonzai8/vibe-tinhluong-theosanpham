package com.plywood.payroll.modules.attendance.service;

import com.plywood.payroll.modules.attendance.dto.request.DailyAttendanceRequest;
import com.plywood.payroll.modules.attendance.dto.response.DailyAttendanceResponse;
import com.plywood.payroll.modules.attendance.entity.DailyAttendance;
import com.plywood.payroll.modules.attendance.entity.AttendanceDefinition;
import com.plywood.payroll.modules.attendance.repository.AttendanceDefinitionRepository;
import com.plywood.payroll.modules.attendance.repository.DailyAttendanceRepository;
import com.plywood.payroll.modules.employee.entity.Employee;
import com.plywood.payroll.modules.employee.repository.EmployeeRepository;
import com.plywood.payroll.modules.organization.entity.Team;
import com.plywood.payroll.modules.organization.repository.TeamRepository;
import com.plywood.payroll.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyAttendanceService {

    private final DailyAttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final TeamRepository teamRepository;
    private final AttendanceDefinitionRepository definitionRepository;

    public List<DailyAttendanceResponse> getByFilters(LocalDate fromDate, LocalDate toDate, Integer month, Integer year, LocalDate date, List<Long> departmentIds, List<Long> teamIds) {
        Specification<DailyAttendance> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("attendanceDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("attendanceDate"), toDate));
            }
            if (month != null && year != null) {
                LocalDate firstDay = LocalDate.of(year, month, 1);
                LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
                predicates.add(cb.between(root.get("attendanceDate"), firstDay, lastDay));
            } else if (month != null) {
                // If only month is provided, we can't easily use between without a year,
                // but usually month and year are provided together in this context.
                // For safety, fallback to function if only month is provided, or handle as needed.
                predicates.add(cb.equal(cb.function("MONTH", Integer.class, root.get("attendanceDate")), month));
            } else if (year != null) {
                predicates.add(cb.equal(cb.function("YEAR", Integer.class, root.get("attendanceDate")), year));
            }
            if (date != null) {
                predicates.add(cb.equal(root.get("attendanceDate"), date));
            }
            if (departmentIds != null && !departmentIds.isEmpty()) {
                predicates.add(root.get("employee").get("department").get("id").in(departmentIds));
            }
            if (teamIds != null && !teamIds.isEmpty()) {
                predicates.add(root.get("originalTeam").get("id").in(teamIds));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return attendanceRepository.findAll(spec).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public List<DailyAttendanceResponse> getByDate(LocalDate date) {
        return attendanceRepository.findByAttendanceDate(date).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public DailyAttendanceResponse getById(Long id) {
        return attendanceRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Điểm danh", id));
    }

    @Transactional
    public DailyAttendanceResponse create(DailyAttendanceRequest request) {
        return mapToResponse(attendanceRepository.save(mapRequestToEntity(request, new DailyAttendance())));
    }

    @Transactional
    public DailyAttendanceResponse update(Long id, DailyAttendanceRequest request) {
        DailyAttendance att = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Điểm danh", id));
        return mapToResponse(attendanceRepository.save(mapRequestToEntity(request, att)));
    }
    
    @Transactional
    public List<DailyAttendanceResponse> saveBatch(List<DailyAttendanceRequest> requests) {
        List<DailyAttendance> attendances = requests.stream()
                .map(req -> mapRequestToEntity(req, new DailyAttendance()))
                .collect(Collectors.toList());
                
        return attendanceRepository.saveAll(attendances).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        if (!attendanceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Điểm danh", id);
        }
        attendanceRepository.deleteById(id);
    }
    
    private DailyAttendance mapRequestToEntity(DailyAttendanceRequest request, DailyAttendance att) {
        Employee emp = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên", request.getEmployeeId()));
                
        Team origTeam = null;
        if (request.getOriginalTeamId() != null) {
            origTeam = teamRepository.findById(request.getOriginalTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tổ biên chế", request.getOriginalTeamId()));
        }
        
        Team actualTeam = null;
        if (request.getActualTeamId() != null) {
            actualTeam = teamRepository.findById(request.getActualTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tổ thực tế", request.getActualTeamId()));
        }
        
        att.setAttendanceDate(request.getAttendanceDate());
        att.setEmployee(emp);
        att.setOriginalTeam(origTeam);
        att.setActualTeam(actualTeam);
        
        if (request.getAttendanceDefinitionId() != null) {
            AttendanceDefinition def = definitionRepository.findById(request.getAttendanceDefinitionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Loại công", request.getAttendanceDefinitionId()));
            att.setAttendanceDefinition(def);
        } else {
            // Mặc định là Ca ngày (NG) nếu không truyền vào
            definitionRepository.findByCode("NG").ifPresent(att::setAttendanceDefinition);
        }
        
        return att;
    }

    public DailyAttendanceResponse mapToResponse(DailyAttendance entity) {
        if (entity == null) return null;
        
        Employee emp = entity.getEmployee();
        Team origTeam = entity.getOriginalTeam();
        Team actualTeam = entity.getActualTeam();
        AttendanceDefinition def = entity.getAttendanceDefinition();
        
        return DailyAttendanceResponse.builder()
                .id(entity.getId())
                .attendanceDate(entity.getAttendanceDate())
                .employeeId(emp != null ? emp.getId() : null)
                .employeeFullName(emp != null ? emp.getFullName() : null)
                .employeeCode(emp != null ? emp.getCode() : null)
                .employeeDepartmentId(emp != null && emp.getDepartment() != null ? emp.getDepartment().getId() : null)
                .employeeTeamId(origTeam != null ? origTeam.getId() : null)
                .originalTeamId(origTeam != null ? origTeam.getId() : null)
                .originalTeamName(origTeam != null ? origTeam.getName() : null)
                .actualTeamId(actualTeam != null ? actualTeam.getId() : null)
                .actualTeamName(actualTeam != null ? actualTeam.getName() : null)
                .attendanceDefinitionId(def != null ? def.getId() : null)
                .attendanceDefinitionCode(def != null ? def.getCode() : null)
                .attendanceDefinitionName(def != null ? def.getName() : null)
                .attendanceDefinitionMultiplier(def != null ? def.getMultiplier() : 1.0)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
