package com.plywood.payroll.modules.employee.service;

import com.plywood.payroll.modules.organization.entity.Department;
import com.plywood.payroll.modules.organization.entity.Role;
import com.plywood.payroll.modules.organization.service.RoleService;
import com.plywood.payroll.modules.organization.service.DepartmentService;

import com.plywood.payroll.modules.employee.dto.request.EmployeeRequest;
import com.plywood.payroll.modules.employee.dto.response.EmployeeResponse;
import com.plywood.payroll.modules.employee.dto.response.EmployeeAuditLogResponse;
import com.plywood.payroll.modules.organization.dto.response.TeamResponse;
import com.plywood.payroll.modules.organization.dto.response.DepartmentResponse;
import com.plywood.payroll.modules.employee.entity.Employee;
import com.plywood.payroll.modules.organization.entity.Team;
import com.plywood.payroll.modules.employee.dto.request.EmployeeNoteRequest;
import com.plywood.payroll.modules.employee.dto.response.*;
import com.plywood.payroll.modules.employee.repository.EmployeeNoteRepository;
import com.plywood.payroll.modules.employee.entity.SalaryProcess;
import com.plywood.payroll.modules.employee.entity.TeamProcess;
import com.plywood.payroll.modules.employee.repository.SalaryProcessRepository;
import com.plywood.payroll.modules.employee.repository.TeamProcessRepository;
import com.plywood.payroll.shared.exception.ResourceNotFoundException;
import com.plywood.payroll.modules.organization.repository.DepartmentRepository;
import com.plywood.payroll.modules.employee.repository.EmployeeRepository;
import com.plywood.payroll.modules.employee.repository.EmployeeAuditLogRepository;
import com.plywood.payroll.modules.organization.repository.RoleRepository;
import com.plywood.payroll.modules.organization.repository.TeamRepository;
import com.plywood.payroll.shared.service.FileService;
import com.plywood.payroll.shared.utils.NameUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final RoleService roleService;
    private final FileService fileService;
    private final SalaryProcessRepository salaryProcessRepository;
    private final TeamProcessRepository teamProcessRepository;
    // KHÔNG inject TeamService để tránh circular dependency.
    // Dùng private mapper nội bộ để map Team → TeamResponse

    @Transactional(readOnly = true)
    public List<EmployeeListResponse> getAll(String search) {
        List<Employee> employees;
        if (search != null && search.trim().length() >= 3) {
            String q = "%" + search.trim().toLowerCase() + "%";
            employees = employeeRepository.search(q);
        } else {
            employees = employeeRepository.findAll();
        }

        return employees.stream()
                .map(this::mapToListResponse)
                .collect(Collectors.toList());
    }

    public EmployeeListResponse mapToListResponse(Employee entity) {
        if (entity == null)
            return null;

        LocalDate today = LocalDate.now();
        TeamProcess currentTeamProc = teamProcessRepository.findEffectiveByDate(entity.getId(), today).orElse(null);
        SalaryProcess currentSalary = salaryProcessRepository.findEffectiveByDate(entity.getId(), today).orElse(null);

        Team team = currentTeamProc != null ? currentTeamProc.getTeam() : null;
        Department dept = entity.getDepartment();
        Role role = entity.getRole();

        return EmployeeListResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .zkDeviceId(entity.getZkDeviceId())
                .fullName(entity.getFullName())
                .phone(entity.getPhone())
                .citizenId(entity.getCitizenId())
                .avatarUrl(entity.getAvatarPath())
                .status(entity.getStatus())
                .canLogin(entity.isCanLogin())
                .departmentId(dept != null ? dept.getId()
                        : (team != null && team.getDepartment() != null ? team.getDepartment().getId() : null))
                .departmentName(dept != null ? dept.getName()
                        : (team != null && team.getDepartment() != null ? team.getDepartment().getName() : null))
                .teamId(team != null ? team.getId() : null)
                .teamName(team != null ? team.getName() : null)
                .roleId(role != null ? role.getId() : null)
                .roleName(role != null ? role.getName() : null)
                .salaryType(currentSalary != null ? currentSalary.getSalaryType().name() : null)
                .baseSalaryConfig(currentSalary != null ? currentSalary.getBaseSalary() : java.math.BigDecimal.ZERO)
                .insuranceSalaryConfig(
                        currentSalary != null ? currentSalary.getInsuranceSalary() : java.math.BigDecimal.ZERO)
                .build();
    }

    @Transactional(readOnly = true)
    public List<EmployeeBasicResponse> findUnrecordedEmployees(LocalDate date, Long teamId, String search, int limit) {
        String searchPattern = "%%";
        Long effectiveTeamId = teamId;

        if (search != null && !search.trim().isEmpty()) {
            searchPattern = "%" + search.trim().toLowerCase() + "%";
            effectiveTeamId = null;
        }

        return employeeRepository.findUnrecordedEmployeesNative(date, effectiveTeamId, searchPattern, limit, 0).stream()
                .map(row -> EmployeeBasicResponse.builder()
                        .id(((Number) row[0]).longValue())
                        .code((String) row[1])
                        .fullName((String) row[2])
                        .teamName((String) row[3])
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EmployeeBasicResponse> searchBasic(String search) {
        if (search == null || search.trim().length() < 3) {
            return java.util.Collections.emptyList();
        }
        String q = "%" + search.trim().toLowerCase() + "%";
        return employeeRepository.search(q).stream()
                .map(e -> EmployeeBasicResponse.builder()
                        .id(e.getId())
                        .code(e.getCode())
                        .fullName(e.getFullName())
                        .build())
                .collect(Collectors.toList());
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

    private EmployeeAuditLogResponse mapToAuditResponse(
            com.plywood.payroll.modules.employee.entity.EmployeeAuditLog entity) {
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

        Employee saved = employeeRepository.save(employee);

        // Tạo tiến trình lương ban đầu
        SalaryProcess sp = new SalaryProcess();
        sp.setEmployee(saved);
        sp.setSalaryType(request.getSalaryType() != null ? request.getSalaryType()
                : com.plywood.payroll.modules.employee.entity.SalaryType.PRODUCT_BASED);
        sp.setBaseSalary(
                request.getBaseSalaryConfig() != null ? request.getBaseSalaryConfig() : java.math.BigDecimal.ZERO);
        sp.setInsuranceSalary(request.getInsuranceSalaryConfig() != null ? request.getInsuranceSalaryConfig()
                : java.math.BigDecimal.ZERO);
        sp.setStartDate(request.getJoinDate() != null ? request.getJoinDate() : LocalDate.now());
        salaryProcessRepository.save(sp);

        // Tạo tiến trình tổ đội ban đầu
        if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tổ sản xuất", request.getTeamId()));
            TeamProcess tp = new TeamProcess();
            tp.setEmployee(saved);
            tp.setTeam(team);
            tp.setStartDate(request.getJoinDate() != null ? request.getJoinDate() : LocalDate.now());
            teamProcessRepository.save(tp);
        }

        return mapToResponse(saved);
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
                    if (seq > maxSeq)
                        maxSeq = seq;
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return prefix + (maxSeq + 1);
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên", id));

        LocalDate today = LocalDate.now();
        SalaryProcess currentSalary = salaryProcessRepository.findEffectiveByDate(id, today).orElse(null);
        TeamProcess currentTeamProc = teamProcessRepository.findEffectiveByDate(id, today).orElse(null);

        String oldPhone = employee.getPhone();
        String oldCitizenId = employee.getCitizenId();
        String oldTeamName = currentTeamProc != null ? currentTeamProc.getTeam().getName() : "N/A";

        mapRequestToEntity(request, employee);
        Employee saved = employeeRepository.save(employee);

        // Logic xử lý lịch sử Lương
        boolean salaryChanged = false;
        if (currentSalary == null ||
                currentSalary.getSalaryType() != request.getSalaryType() ||
                currentSalary.getBaseSalary()
                        .compareTo(request.getBaseSalaryConfig() != null ? request.getBaseSalaryConfig()
                                : java.math.BigDecimal.ZERO) != 0
                ||
                currentSalary.getInsuranceSalary()
                        .compareTo(request.getInsuranceSalaryConfig() != null ? request.getInsuranceSalaryConfig()
                                : java.math.BigDecimal.ZERO) != 0) {

            salaryChanged = true;
            if (currentSalary != null && currentSalary.getStartDate().equals(today)) {
                // Nếu đã có bản ghi bắt đầu từ hôm nay, cập nhật luôn bản ghi đó
                currentSalary.setSalaryType(request.getSalaryType() != null ? request.getSalaryType()
                        : com.plywood.payroll.modules.employee.entity.SalaryType.PRODUCT_BASED);
                currentSalary.setBaseSalary(request.getBaseSalaryConfig() != null ? request.getBaseSalaryConfig()
                        : java.math.BigDecimal.ZERO);
                currentSalary.setInsuranceSalary(
                        request.getInsuranceSalaryConfig() != null ? request.getInsuranceSalaryConfig()
                                : java.math.BigDecimal.ZERO);
                salaryProcessRepository.save(currentSalary);
            } else {
                if (currentSalary != null) {
                    currentSalary.setEndDate(today.minusDays(1));
                    salaryProcessRepository.save(currentSalary);
                }
                SalaryProcess nextSalary = new SalaryProcess();
                nextSalary.setEmployee(saved);
                nextSalary.setSalaryType(request.getSalaryType() != null ? request.getSalaryType()
                        : com.plywood.payroll.modules.employee.entity.SalaryType.PRODUCT_BASED);
                nextSalary.setBaseSalary(request.getBaseSalaryConfig() != null ? request.getBaseSalaryConfig()
                        : java.math.BigDecimal.ZERO);
                nextSalary.setInsuranceSalary(
                        request.getInsuranceSalaryConfig() != null ? request.getInsuranceSalaryConfig()
                                : java.math.BigDecimal.ZERO);
                nextSalary.setStartDate(today);
                salaryProcessRepository.save(nextSalary);
            }
        }

        // Logic xử lý lịch sử Tổ đội
        if (request.getTeamId() != null
                && (currentTeamProc == null || !currentTeamProc.getTeam().getId().equals(request.getTeamId()))) {
            Team newTeamEntity = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tổ sản xuất", request.getTeamId()));

            if (currentTeamProc != null && currentTeamProc.getStartDate().equals(today)) {
                // Cập nhật luôn nếu bản ghi hiện tại cũng bắt đầu từ hôm nay
                currentTeamProc.setTeam(newTeamEntity);
                teamProcessRepository.save(currentTeamProc);
            } else {
                if (currentTeamProc != null) {
                    currentTeamProc.setEndDate(today.minusDays(1));
                    teamProcessRepository.save(currentTeamProc);
                }
                TeamProcess nextTeamProc = new TeamProcess();
                nextTeamProc.setEmployee(saved);
                nextTeamProc.setTeam(newTeamEntity);
                nextTeamProc.setStartDate(today);
                teamProcessRepository.save(nextTeamProc);
            }

            logChange(saved, "UPDATE_TEAM", "team", oldTeamName, newTeamEntity.getName(), "ADMIN");
        } else if (request.getTeamId() == null && currentTeamProc != null) {
            if (currentTeamProc.getStartDate().equals(today)) {
                teamProcessRepository.delete(currentTeamProc);
            } else {
                currentTeamProc.setEndDate(today.minusDays(1));
                teamProcessRepository.save(currentTeamProc);
            }
            logChange(saved, "UPDATE_TEAM", "team", oldTeamName, "N/A", "ADMIN");
        }

        logChange(saved, "UPDATE_PROFILE", "phone", oldPhone, saved.getPhone(), "ADMIN");
        logChange(saved, "UPDATE_PROFILE", "citizenId", oldCitizenId, saved.getCitizenId(), "ADMIN");
        if (salaryChanged) {
            logChange(saved, "UPDATE_SALARY", "salaryConfig", "OLD", "NEW", "ADMIN");
        }

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
        if ((oldVal == null && newVal == null) || (oldVal != null && oldVal.equals(newVal)))
            return;

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
    public String updateAvatar(Long id, org.springframework.web.multipart.MultipartFile file)
            throws java.io.IOException {
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
        employee.setZkDeviceId(request.getZkDeviceId());
        employee.setGender(request.getGender());
        employee.setDob(request.getDob());
        employee.setJoinDate(request.getJoinDate());
        employee.setInsuranceStartDate(request.getInsuranceStartDate());
        employee.setCitizenIdIssuedDate(request.getCitizenIdIssuedDate());
        employee.setCitizenIdIssuedPlace(request.getCitizenIdIssuedPlace());
        employee.setBirthAddress(request.getBirthAddress());
        employee.setPermanentAddress(request.getPermanentAddress());
        employee.setNotes(request.getNotes());

        if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tổ sản xuất", request.getTeamId()));
            // Lưu bộ phận thuộc tổ đó vào Employee
            if (team.getDepartment() != null) {
                employee.setDepartment(team.getDepartment());
            }
        }

        if (request.getDepartmentId() != null) {
            employee.setDepartment(departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Phòng ban", request.getDepartmentId())));
        }

        if (request.getRoleId() != null) {
            employee.setRole(roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chức vụ", request.getRoleId())));
        } else {
            employee.setRole(null);
        }
    }

    @Transactional
    public void updateZkDeviceId(Long id, String zkDeviceId) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên", id));
        employee.setZkDeviceId(zkDeviceId);
        employeeRepository.save(employee);
    }

    public EmployeeResponse mapToResponse(Employee entity) {
        if (entity == null)
            return null;
        EmployeeResponse response = new EmployeeResponse();
        response.setId(entity.getId());
        response.setCode(entity.getCode());
        response.setZkDeviceId(entity.getZkDeviceId());
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

        // Map team và lương từ lịch sử (lấy bản ghi đang có hiệu lực)
        LocalDate today = LocalDate.now();
        TeamProcess currentTeamProc = teamProcessRepository.findEffectiveByDate(entity.getId(), today).orElse(null);
        SalaryProcess currentSalary = salaryProcessRepository.findEffectiveByDate(entity.getId(), today).orElse(null);

        response.setTeam(currentTeamProc != null ? mapTeamShallow(currentTeamProc.getTeam()) : null);
        response.setDepartment(mapDepartmentShallow(entity.getDepartment()));
        response.setRole(roleService.mapToResponse(entity.getRole()));

        if (currentSalary != null) {
            response.setSalaryType(currentSalary.getSalaryType());
            response.setBaseSalaryConfig(currentSalary.getBaseSalary());
            response.setInsuranceSalaryConfig(currentSalary.getInsuranceSalary());
        } else {
            response.setSalaryType(com.plywood.payroll.modules.employee.entity.SalaryType.PRODUCT_BASED);
            response.setBaseSalaryConfig(java.math.BigDecimal.ZERO);
            response.setInsuranceSalaryConfig(java.math.BigDecimal.ZERO);
        }

        return response;
    }

    private DepartmentResponse mapDepartmentShallow(Department entity) {
        if (entity == null)
            return null;
        DepartmentResponse res = new DepartmentResponse();
        res.setId(entity.getId());
        res.setName(entity.getName());
        return res;
    }

    /**
     * Mapper gọn nhẹ: Team → TeamResponse, KHÔNG gọi TeamService để tránh circular
     * dep.
     * Không include memberCount (tránh lazy loading N+1), chỉ cần id + name +
     * department.
     */
    private TeamResponse mapTeamShallow(Team team) {
        if (team == null)
            return null;
        Department dept = team.getDepartment();
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .departmentId(dept != null ? dept.getId() : null)
                .departmentName(dept != null ? dept.getName() : null)
                .build();
    }

    public List<SalaryProcessResponse> getSalaryHistory(Long id) {
        return salaryProcessRepository.findByEmployeeIdOrderByStartDateDesc(id).stream()
                .map(this::mapToSalaryProcessResponse)
                .collect(Collectors.toList());
    }

    public List<TeamProcessResponse> getTeamHistory(Long id) {
        return teamProcessRepository.findByEmployeeIdOrderByStartDateDesc(id).stream()
                .map(this::mapToTeamProcessResponse)
                .collect(Collectors.toList());
    }

    private SalaryProcessResponse mapToSalaryProcessResponse(SalaryProcess entity) {
        SalaryProcessResponse res = new SalaryProcessResponse();
        res.setId(entity.getId());
        res.setSalaryType(entity.getSalaryType());
        res.setBaseSalary(entity.getBaseSalary());
        res.setInsuranceSalary(entity.getInsuranceSalary());
        res.setStartDate(entity.getStartDate());
        res.setEndDate(entity.getEndDate());
        res.setCurrent(entity.getEndDate() == null);
        return res;
    }

    private TeamProcessResponse mapToTeamProcessResponse(TeamProcess entity) {
        TeamProcessResponse res = new TeamProcessResponse();
        res.setId(entity.getId());
        res.setTeam(mapTeamShallow(entity.getTeam()));
        res.setStartDate(entity.getStartDate());
        res.setEndDate(entity.getEndDate());
        res.setCurrent(entity.getEndDate() == null);
        return res;
    }
}
