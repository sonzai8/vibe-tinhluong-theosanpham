package com.plywood.payroll.modules.employee.service;
import com.plywood.payroll.modules.organization.service.RoleService;
import com.plywood.payroll.modules.organization.service.DepartmentService;
import com.plywood.payroll.modules.organization.service.TeamService;

import com.plywood.payroll.modules.employee.dto.request.EmployeeRequest;
import com.plywood.payroll.modules.employee.dto.response.EmployeeResponse;
import com.plywood.payroll.modules.employee.dto.response.EmployeeAuditLogResponse;
import com.plywood.payroll.modules.organization.dto.response.TeamResponse;
import com.plywood.payroll.modules.employee.entity.Employee;
import com.plywood.payroll.modules.organization.entity.Team;
import com.plywood.payroll.shared.exception.ResourceNotFoundException;
import com.plywood.payroll.modules.organization.repository.DepartmentRepository;
import com.plywood.payroll.modules.employee.repository.EmployeeRepository;
import com.plywood.payroll.modules.employee.repository.EmployeeAuditLogRepository;
import com.plywood.payroll.modules.organization.repository.RoleRepository;
import com.plywood.payroll.modules.organization.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeAuditLogRepository employeeAuditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final TeamRepository teamRepository;
    private final DepartmentService departmentService;
    private final RoleService roleService;
    // KHÔNG inject TeamService để tránh circular dependency.
    // Dùng private mapper nội bộ để map Team → TeamResponse

    public List<EmployeeResponse> getAll() {
        return employeeRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<EmployeeAuditLogResponse> getAuditLogs(Long employeeId) {
        return employeeAuditLogRepository.findByEmployeeIdOrderByChangedAtDesc(employeeId).stream()
                .map(this::mapToAuditResponse)
                .collect(Collectors.toList());
    }

    private EmployeeAuditLogResponse mapToAuditResponse(com.plywood.payroll.modules.employee.entity.EmployeeAuditLog entity) {
        return EmployeeAuditLogResponse.builder()
                .id(entity.getId())
                .action(entity.getAction())
                .fieldName(entity.getFieldName())
                .oldValue(entity.getOldValue())
                .newValue(entity.getNewValue())
                .changedBy(entity.getChangedBy())
                .changedAt(entity.getChangedAt())
                .build();
    }

    public EmployeeResponse getById(Long id) {
        return employeeRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên", id));
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        Employee employee = new Employee();
        mapRequestToEntity(request, employee);
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            employee.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        return mapToResponse(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên", id));
        String oldPhone = employee.getPhone();
        String oldCitizenId = employee.getCitizenId();
        String oldTeam = employee.getTeam() != null ? employee.getTeam().getName() : null;

        mapRequestToEntity(request, employee);
        Employee saved = employeeRepository.save(employee);

        logChange(saved, "UPDATE_PROFILE", "phone", oldPhone, saved.getPhone(), "ADMIN");
        logChange(saved, "UPDATE_PROFILE", "citizenId", oldCitizenId, saved.getCitizenId(), "ADMIN");
        String newTeam = saved.getTeam() != null ? saved.getTeam().getName() : null;
        logChange(saved, "UPDATE_TEAM", "team", oldTeam, newTeam, "ADMIN");

        return mapToResponse(saved);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên", id));
        employee.setPassword(passwordEncoder.encode(newPassword));
        employeeRepository.save(employee);
        
        logChange(employee, "RESET_PASSWORD", "password", "***", "***", "ADMIN");
    }

    private void logChange(Employee emp, String action, String field, String oldVal, String newVal, String by) {
        if ((oldVal == null && newVal == null) || (oldVal != null && oldVal.equals(newVal))) return;
        
        com.plywood.payroll.modules.employee.entity.EmployeeAuditLog log = new com.plywood.payroll.modules.employee.entity.EmployeeAuditLog();
        log.setEmployee(emp);
        log.setAction(action);
        log.setFieldName(field);
        log.setOldValue(oldVal);
        log.setNewValue(newVal);
        log.setChangedBy(by);
        employeeAuditLogRepository.save(log);
    }

    @Transactional
    public void delete(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Nhân viên", id);
        }
        employeeRepository.deleteById(id);
    }

    private void mapRequestToEntity(EmployeeRequest request, Employee employee) {
        employee.setCode(request.getCode());
        employee.setFullName(request.getFullName());
        employee.setStatus(request.getStatus());
        employee.setUsername(request.getUsername());
        employee.setCanLogin(request.isCanLogin());
        employee.setPhone(request.getPhone());
        employee.setCitizenId(request.getCitizenId());

        // Gán Tổ trực tiếp
        if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tổ sản xuất", request.getTeamId()));
            employee.setTeam(team);
            // Tự động đặt department theo tổ (nếu team có department)
            if (team.getDepartment() != null) {
                employee.setDepartment(team.getDepartment());
            }
        } else {
            employee.setTeam(null);
        }

        if (request.getDepartmentId() != null) {
            employee.setDepartment(departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Phòng ban", request.getDepartmentId())));
        } else if (request.getTeamId() == null) {
            employee.setDepartment(null);
        }

        if (request.getRoleId() != null) {
            employee.setRole(roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chức vụ", request.getRoleId())));
        } else {
            employee.setRole(null);
        }
    }

    public EmployeeResponse mapToResponse(Employee entity) {
        if (entity == null) return null;
        EmployeeResponse response = new EmployeeResponse();
        response.setId(entity.getId());
        response.setCode(entity.getCode());
        response.setFullName(entity.getFullName());
        response.setStatus(entity.getStatus());
        response.setUsername(entity.getUsername());
        response.setCanLogin(entity.isCanLogin());
        response.setPhone(entity.getPhone());
        response.setCitizenId(entity.getCitizenId());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        // Map team bằng mapper nội bộ nhẹ (không include members để tránh recursion)
        response.setTeam(mapTeamShallow(entity.getTeam()));
        response.setDepartment(departmentService.mapToResponse(entity.getDepartment()));
        response.setRole(roleService.mapToResponse(entity.getRole()));

        return response;
    }

    /**
     * Mapper gọn nhẹ: Team → TeamResponse, KHÔNG gọi TeamService để tránh circular dep.
     * Không include memberCount (tránh lazy loading N+1), chỉ cần id + name + department.
     */
    private TeamResponse mapTeamShallow(Team team) {
        if (team == null) return null;
        TeamResponse r = new TeamResponse();
        r.setId(team.getId());
        r.setName(team.getName());
        r.setDepartment(departmentService.mapToResponse(team.getDepartment()));
        return r;
    }
}
