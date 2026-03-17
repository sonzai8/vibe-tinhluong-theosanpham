package com.plywood.payroll.modules.organization.service;
import com.plywood.payroll.modules.production.service.ProductionStepService;
import com.plywood.payroll.modules.employee.service.EmployeeService;

import com.plywood.payroll.modules.organization.dto.request.TeamMemberRequest;
import com.plywood.payroll.modules.organization.dto.request.TeamRequest;
import com.plywood.payroll.modules.organization.dto.response.TeamMemberResponse;
import com.plywood.payroll.modules.organization.dto.response.TeamResponse;
import com.plywood.payroll.modules.organization.entity.Department;
import com.plywood.payroll.modules.employee.entity.Employee;
import com.plywood.payroll.modules.production.entity.ProductionStep;
import com.plywood.payroll.modules.organization.entity.Team;
import com.plywood.payroll.modules.organization.entity.TeamMember;
import com.plywood.payroll.shared.exception.BusinessException;
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
    private final ProductionStepService productionStepService;
    private final EmployeeService employeeService;
    private final DepartmentService departmentService;

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
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Tổ sản xuất", teamId));
        return team.getMembers().stream()
                .map(this::mapMemberToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TeamMemberResponse addMember(Long teamId, TeamMemberRequest request) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Tổ sản xuất", teamId));
                
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên", request.getEmployeeId()));
                
        // Check if employee already in a team
        boolean alreadyInTeam = team.getMembers().stream()
                .anyMatch(m -> m.getEmployee().getId().equals(employee.getId()));
                
        if (alreadyInTeam) {
            throw new BusinessException("Nhân viên " + employee.getFullName() + " đã có trong tổ này.");
        }
        
        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setEmployee(employee);
        member.setJoinedDate(request.getJoinedDate());
        
        team.getMembers().add(member);
        teamRepository.save(team);
        
        return mapMemberToResponse(member);
    }

    @Transactional
    public void removeMember(Long teamId, Long employeeId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Tổ sản xuất", teamId));
                
        boolean removed = team.getMembers().removeIf(m -> m.getEmployee().getId().equals(employeeId));
        if (!removed) {
            throw new ResourceNotFoundException("Nhân viên trong tổ", employeeId);
        }
        
        teamRepository.save(team);
    }

    public TeamResponse mapToResponse(Team entity) {
        if (entity == null) return null;
        TeamResponse response = new TeamResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDepartment(departmentService.mapToResponse(entity.getDepartment()));
        response.setProductionStep(productionStepService.mapToResponse(entity.getProductionStep()));
        
        // Thống kê thành viên dựa trên quan hệ trực tiếp ở Employee
        if (employeeRepository != null) {
            java.util.List<Employee> teamEmps = employeeRepository.findAllByTeamId(entity.getId());
            response.setMemberCount(teamEmps.size());
            response.setMemberNames(teamEmps.stream()
                .map(Employee::getFullName)
                .collect(java.util.stream.Collectors.toList()));
        } else {
            response.setMemberCount(0);
            response.setMemberNames(new java.util.ArrayList<>());
        }
        
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
    
    public TeamMemberResponse mapMemberToResponse(TeamMember entity) {
        if (entity == null) return null;
        TeamMemberResponse response = new TeamMemberResponse();
        response.setId(entity.getId());
        response.setEmployee(employeeService.mapToResponse(entity.getEmployee()));
        response.setJoinedDate(entity.getJoinedDate());
        return response;
    }
}
