package com.plywood.payroll.modules.organization.service;
import com.plywood.payroll.modules.organization.repository.TeamRepository;
import com.plywood.payroll.modules.organization.entity.Team;

import com.plywood.payroll.modules.organization.dto.request.DepartmentRequest;
import com.plywood.payroll.modules.organization.dto.response.DepartmentResponse;
import com.plywood.payroll.modules.organization.entity.Department;
import com.plywood.payroll.shared.exception.ResourceNotFoundException;
import com.plywood.payroll.modules.organization.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final com.plywood.payroll.modules.organization.repository.TeamRepository teamRepository;

    public List<DepartmentResponse> getAll() {
        return departmentRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public DepartmentResponse getById(Long id) {
        return departmentRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Phòng ban", id));
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        Department department = new Department();
        department.setName(request.getName());
        return mapToResponse(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phòng ban", id));
        department.setName(request.getName());
        return mapToResponse(departmentRepository.save(department));
    }

    @Transactional
    public void delete(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Phòng ban", id);
        }
        departmentRepository.deleteById(id);
    }

    public DepartmentResponse mapToResponse(Department entity) {
        if (entity == null) return null;
        DepartmentResponse response = new DepartmentResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        
        if (teamRepository != null) {
            java.util.List<com.plywood.payroll.modules.organization.entity.Team> teams = teamRepository.findAll().stream()
                .filter(t -> t.getDepartment() != null && t.getDepartment().getId().equals(entity.getId()))
                .collect(java.util.stream.Collectors.toList());
            response.setTeamCount(teams.size());
            response.setTeamNames(teams.stream().map(com.plywood.payroll.modules.organization.entity.Team::getName).collect(java.util.stream.Collectors.toList()));
        }

        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
