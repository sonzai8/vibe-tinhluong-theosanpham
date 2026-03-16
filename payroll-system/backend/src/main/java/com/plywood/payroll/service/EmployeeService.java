package com.plywood.payroll.service;

import com.plywood.payroll.dto.request.EmployeeRequest;
import com.plywood.payroll.dto.response.EmployeeResponse;
import com.plywood.payroll.entity.Employee;
import com.plywood.payroll.exception.ResourceNotFoundException;
import com.plywood.payroll.repository.DepartmentRepository;
import com.plywood.payroll.repository.EmployeeRepository;
import com.plywood.payroll.repository.RoleRepository;
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
    private final DepartmentService departmentService;
    private final RoleService roleService;

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
        
        // Không map password ở đây vì cần mã hóa (thiết lập ở tầng auth/user mgmt sau nếu cần)

        if (request.getDepartmentId() != null) {
            employee.setDepartment(departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Phòng ban", request.getDepartmentId())));
        } else {
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

        response.setDepartment(departmentService.mapToResponse(entity.getDepartment()));
        response.setRole(roleService.mapToResponse(entity.getRole()));

        return response;
    }
}
