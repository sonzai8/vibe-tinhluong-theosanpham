package com.plywood.payroll.service;

import com.plywood.payroll.dto.request.EmployeeRequest;
import com.plywood.payroll.dto.response.EmployeeResponse;
import com.plywood.payroll.dto.response.TeamResponse;
import com.plywood.payroll.entity.Employee;
import com.plywood.payroll.entity.Team;
import com.plywood.payroll.exception.ResourceNotFoundException;
import com.plywood.payroll.repository.DepartmentRepository;
import com.plywood.payroll.repository.EmployeeRepository;
import com.plywood.payroll.repository.RoleRepository;
import com.plywood.payroll.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
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

    public EmployeeResponse getById(Long id) {
        return employeeRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên", id));
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        Employee employee = new Employee();
        mapRequestToEntity(request, employee);
        return mapToResponse(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên", id));
        mapRequestToEntity(request, employee);
        return mapToResponse(employeeRepository.save(employee));
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
