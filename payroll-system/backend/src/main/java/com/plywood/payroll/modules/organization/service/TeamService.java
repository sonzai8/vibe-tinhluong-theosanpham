package com.plywood.payroll.modules.organization.service;

import com.plywood.payroll.modules.employee.entity.TeamProcess;
import com.plywood.payroll.modules.employee.repository.TeamProcessRepository;
import com.plywood.payroll.modules.employee.service.EmployeeService;

import com.plywood.payroll.modules.organization.dto.request.TeamMemberRequest;
import com.plywood.payroll.modules.organization.dto.request.TeamRequest;
import com.plywood.payroll.modules.organization.dto.response.TeamMemberResponse;
import com.plywood.payroll.modules.organization.dto.response.TeamResponse;
import com.plywood.payroll.modules.organization.entity.Department;
import com.plywood.payroll.modules.employee.entity.Employee;
import com.plywood.payroll.modules.production.entity.ProductionStep;
import com.plywood.payroll.modules.organization.entity.Team;
import com.plywood.payroll.shared.exception.ResourceNotFoundException;
import com.plywood.payroll.modules.organization.repository.DepartmentRepository;
import com.plywood.payroll.modules.employee.repository.EmployeeRepository;
import com.plywood.payroll.modules.production.repository.ProductionStepRepository;
import com.plywood.payroll.modules.organization.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final ProductionStepRepository productionStepRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final TeamProcessRepository teamProcessRepository;
    private final EmployeeService employeeService;

    public List<TeamResponse> getAll() {
        return teamRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public TeamResponse getById(Long id) {
        return teamRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Tổ sản xuất", id));
    }

    @Transactional
    public TeamResponse create(TeamRequest request) {
        Team team = new Team();
        team.setName(request.getName());
        
        ProductionStep step = productionStepRepository.findById(request.getProductionStepId())
                .orElseThrow(() -> new ResourceNotFoundException("Công đoạn", request.getProductionStepId()));
        team.setProductionStep(step);

        // Gán phòng ban nếu có
        if (request.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Phòng ban", request.getDepartmentId()));
            team.setDepartment(dept);
        }
        
        return mapToResponse(teamRepository.save(team));
    }

    @Transactional
    public TeamResponse update(Long id, TeamRequest request) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tổ sản xuất", id));
                
        team.setName(request.getName());
        
        ProductionStep step = productionStepRepository.findById(request.getProductionStepId())
                .orElseThrow(() -> new ResourceNotFoundException("Công đoạn", request.getProductionStepId()));
        team.setProductionStep(step);

        // Cập nhật phòng ban
        if (request.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Phòng ban", request.getDepartmentId()));
            team.setDepartment(dept);
        } else {
            team.setDepartment(null);
        }
        
        return mapToResponse(teamRepository.save(team));
    }

    @Transactional
    public void delete(Long id) {
        if (!teamRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tổ sản xuất", id);
        }
        teamRepository.deleteById(id);
    }
    
    // --- Member Management ---

    public List<TeamMemberResponse> getMembers(Long teamId) {
        teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Tổ sản xuất", teamId));
        return teamProcessRepository.findEffectiveByTeamId(teamId, java.time.LocalDate.now()).stream()
                .map(this::mapProcessToMemberResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TeamMemberResponse addMember(Long teamId, TeamMemberRequest request) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Tổ sản xuất", teamId));
        
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên", request.getEmployeeId()));

        // Logic tương đương việc chuyển tổ cho nhân viên
        java.time.LocalDate today = java.time.LocalDate.now();
        teamProcessRepository.findEffectiveByDate(employee.getId(), today).ifPresent(tp -> {
            tp.setEndDate(today.minusDays(1));
            teamProcessRepository.save(tp);
        });

        TeamProcess nextTp = new TeamProcess();
        nextTp.setEmployee(employee);
        nextTp.setTeam(team);
        nextTp.setStartDate(today);
        teamProcessRepository.save(nextTp);

        return mapProcessToMemberResponse(nextTp);
    }

    @Transactional
    public void removeMember(Long teamId, Long employeeId) {
        java.time.LocalDate today = java.time.LocalDate.now();
        teamProcessRepository.findEffectiveByDate(employeeId, today).ifPresent(tp -> {
            if (tp.getTeam().getId().equals(teamId)) {
                tp.setEndDate(today.minusDays(1));
                teamProcessRepository.save(tp);
            }
        });
    }

    private TeamMemberResponse mapProcessToMemberResponse(TeamProcess tp) {
        TeamMemberResponse res = new TeamMemberResponse();
        res.setId(tp.getId());
        res.setEmployee(employeeService.mapToResponse(tp.getEmployee()));
        res.setJoinedDate(tp.getStartDate());
        return res;
    }

    public TeamResponse mapToResponse(Team entity) {
        if (entity == null) return null;
        Department dept = entity.getDepartment();
        ProductionStep step = entity.getProductionStep();

        TeamResponse.TeamResponseBuilder builder = TeamResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .departmentId(dept != null ? dept.getId() : null)
                .departmentName(dept != null ? dept.getName() : null)
                .productionStepId(step != null ? step.getId() : null)
                .productionStepName(step != null ? step.getName() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt());

        // Thống kê thành viên dựa trên TeamProcess (hiệu lực hiện tại)
        if (teamProcessRepository != null) {
            List<TeamProcess> currentProcesses =
                teamProcessRepository.findEffectiveByTeamId(entity.getId(), java.time.LocalDate.now());
            builder.memberCount(currentProcesses.size());
            builder.memberNames(currentProcesses.stream()
                .map(tp -> tp.getEmployee().getFullName())
                .collect(java.util.stream.Collectors.toList()));
        } else {
            builder.memberCount(0);
            builder.memberNames(new java.util.ArrayList<>());
        }

        return builder.build();
    }
    
}
