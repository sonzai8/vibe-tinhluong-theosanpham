package com.plywood.payroll.modules.attendance.service;
import com.plywood.payroll.modules.organization.service.TeamService;
import com.plywood.payroll.modules.employee.service.EmployeeService;

import com.plywood.payroll.modules.attendance.dto.request.DailyAttendanceRequest;
import com.plywood.payroll.modules.attendance.dto.response.DailyAttendanceResponse;
import com.plywood.payroll.modules.attendance.entity.DailyAttendance;
import com.plywood.payroll.modules.attendance.entity.AttendanceDefinition;
import com.plywood.payroll.modules.attendance.repository.AttendanceDefinitionRepository;
import com.plywood.payroll.modules.employee.entity.Employee;
import com.plywood.payroll.modules.organization.entity.Team;
import com.plywood.payroll.shared.exception.ResourceNotFoundException;
import com.plywood.payroll.modules.attendance.repository.DailyAttendanceRepository;
import com.plywood.payroll.modules.employee.repository.EmployeeRepository;
import com.plywood.payroll.modules.organization.repository.TeamRepository;
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
    private final EmployeeService employeeService;
    private final TeamService teamService;
    private final AttendanceDefinitionRepository definitionRepository;
    private final AttendanceDefinitionService definitionService;

    public List<DailyAttendanceResponse> getByFilters(LocalDate fromDate, LocalDate toDate, Integer month, Integer year, LocalDate date, List<Long> departmentIds, List<Long> teamIds) {
        Specification<DailyAttendance> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("attendanceDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("attendanceDate"), toDate));
            }
            if (month != null) {
                predicates.add(cb.equal(cb.function("MONTH", Integer.class, root.get("attendanceDate")), month));
            }
            if (year != null) {
                predicates.add(cb.equal(cb.function("YEAR", Integer.class, root.get("attendanceDate")), year));
            }
            if (date != null) {
                predicates.add(cb.equal(root.get("attendanceDate"), date));
            }
            if (departmentIds != null && !departmentIds.isEmpty()) {
                predicates.add(root.get("employee").get("department").get("id").in(departmentIds));
            }
            if (teamIds != null && !teamIds.isEmpty()) {
                predicates.add(root.get("actualTeam").get("id").in(teamIds));
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
        DailyAttendanceResponse response = new DailyAttendanceResponse();
        response.setId(entity.getId());
        response.setAttendanceDate(entity.getAttendanceDate());
        response.setEmployee(employeeService.mapToResponse(entity.getEmployee()));
        response.setOriginalTeam(teamService.mapToResponse(entity.getOriginalTeam()));
        response.setActualTeam(teamService.mapToResponse(entity.getActualTeam()));
        if (entity.getAttendanceDefinition() != null) {
            response.setAttendanceDefinition(definitionService.mapToResponse(entity.getAttendanceDefinition()));
        }
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
