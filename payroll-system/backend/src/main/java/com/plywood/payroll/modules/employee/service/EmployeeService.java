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
import com.plywood.payroll.modules.employee.dto.request.EmployeeNoteRequest;
import com.plywood.payroll.modules.employee.dto.response.EmployeeNoteResponse;
import com.plywood.payroll.modules.employee.repository.EmployeeNoteRepository;
import com.plywood.payroll.shared.exception.ResourceNotFoundException;
import com.plywood.payroll.modules.organization.repository.DepartmentRepository;
import com.plywood.payroll.modules.employee.repository.EmployeeRepository;
import com.plywood.payroll.modules.employee.repository.EmployeeAuditLogRepository;
import com.plywood.payroll.modules.organization.repository.RoleRepository;
import com.plywood.payroll.modules.organization.repository.TeamRepository;
import com.plywood.payroll.shared.service.FileService;
import com.plywood.payroll.shared.utils.NameUtils;
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
    private final EmployeeNoteRepository employeeNoteRepository;
    private final PasswordEncoder passwordEncoder;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final TeamRepository teamRepository;
    private final DepartmentService departmentService;
    private final RoleService roleService;
    private final FileService fileService;
    // KHÔNG inject TeamService để tránh circular dependency.
    // Dùng private mapper nội bộ để map Team → TeamResponse

    public List<EmployeeResponse> getAll() {
        return employeeRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    public EmployeeResponse getById(Long id) {
        return employeeRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên", id));
    }

    public List<EmployeeAuditLogResponse> getAuditLogs(Long employeeId) {
        return employeeAuditLogRepository.findByEmployeeIdOrderByChangedAtDesc(employeeId).stream()
                .map(this::mapToAuditResponse)
                .collect(Collectors.toList());
    }

    public List<EmployeeNoteResponse> getNotes(Long employeeId) {
        return employeeNoteRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId).stream()
                .map(this::mapNoteToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public EmployeeNoteResponse addNote(Long employeeId, EmployeeNoteRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên", employeeId));
        
        com.plywood.payroll.modules.employee.entity.EmployeeNote note = new com.plywood.payroll.modules.employee.entity.EmployeeNote();
        note.setEmployee(employee);
        note.setContent(request.getContent());
        note.setMonth(request.getMonth());
        note.setYear(request.getYear());
        note.setCreatedBy("ADMIN"); // Có thể lấy từ SecurityContext nếu có
        
        return mapNoteToResponse(employeeNoteRepository.save(note));
    }

    @Transactional
    public void deleteNote(Long noteId) {
        employeeNoteRepository.deleteById(noteId);
    }

    private EmployeeNoteResponse mapNoteToResponse(com.plywood.payroll.modules.employee.entity.EmployeeNote entity) {
        EmployeeNoteResponse r = new EmployeeNoteResponse();
        r.setId(entity.getId());
        r.setContent(entity.getContent());
        r.setMonth(entity.getMonth());
        r.setYear(entity.getYear());
        r.setCreatedBy(entity.getCreatedBy());
        r.setCreatedAt(entity.getCreatedAt());
        return r;
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

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        Employee employee = new Employee();
        mapRequestToEntity(request, employee);
        
        // Tự động sinh mã nếu chưa có
        if (employee.getCode() == null || employee.getCode().trim().isEmpty()) {
            employee.setCode(generateNextEmployeeCode(employee.getFullName()));
        }
        if (employee.getUsername() == null || employee.getUsername().trim().isEmpty()) {
            employee.setUsername(generateNextEmployeeCode(employee.getFullName()));
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            employee.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        return mapToResponse(employeeRepository.save(employee));
    }

    private String generateNextEmployeeCode(String fullName) {
        String prefix = NameUtils.generateCodePrefix(fullName);
        List<String> existingCodes = employeeRepository.findCodesByPrefix(prefix);
        
        int maxSeq = 0;
        for (String code : existingCodes) {
            if (code.length() > prefix.length()) {
                String seqStr = code.substring(prefix.length());
                try {
                    int seq = Integer.parseInt(seqStr);
                    if (seq > maxSeq) maxSeq = seq;
                } catch (NumberFormatException ignored) {}
            }
        }
        
        return prefix + (maxSeq + 1);
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
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên", id));
        employee.setStatus("INACTIVE");
        employeeRepository.save(employee);
        
        logChange(employee, "SOFT_DELETE", "status", "ACTIVE", "INACTIVE", "ADMIN");
    }

    @Transactional
    public String updateAvatar(Long id, org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên", id));

        // Delete old avatar if exists
        if (employee.getAvatarPath() != null) {
            fileService.deleteAvatar(employee.getAvatarPath());
        }

        String fileName = fileService.saveAvatar(file);
        employee.setAvatarPath(fileName);
        employeeRepository.save(employee);

        return "/uploads/avatars/" + fileName;
    }

    private void mapRequestToEntity(EmployeeRequest request, Employee employee) {
        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            employee.setCode(request.getCode());
        }
        employee.setFullName(request.getFullName());
        employee.setStatus(request.getStatus());
        employee.setUsername(request.getUsername());
        employee.setCanLogin(request.isCanLogin());
        employee.setPhone(request.getPhone());
        employee.setCitizenId(request.getCitizenId());
        
        employee.setGender(request.getGender());
        employee.setDob(request.getDob());
        employee.setJoinDate(request.getJoinDate());
        employee.setInsuranceStartDate(request.getInsuranceStartDate());
        employee.setCitizenIdIssuedDate(request.getCitizenIdIssuedDate());
        employee.setCitizenIdIssuedPlace(request.getCitizenIdIssuedPlace());
        employee.setBirthAddress(request.getBirthAddress());
        employee.setPermanentAddress(request.getPermanentAddress());
        employee.setNotes(request.getNotes());

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
        
        response.setGender(entity.getGender());
        response.setDob(entity.getDob());
        response.setJoinDate(entity.getJoinDate());
        response.setInsuranceStartDate(entity.getInsuranceStartDate());
        response.setCitizenIdIssuedDate(entity.getCitizenIdIssuedDate());
        response.setCitizenIdIssuedPlace(entity.getCitizenIdIssuedPlace());
        response.setBirthAddress(entity.getBirthAddress());
        response.setPermanentAddress(entity.getPermanentAddress());
        response.setNotes(entity.getNotes());

        if (entity.getAvatarPath() != null) {
            response.setAvatarUrl("/uploads/avatars/" + entity.getAvatarPath());
        }

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
