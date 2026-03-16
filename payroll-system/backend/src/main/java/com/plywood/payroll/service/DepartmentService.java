package com.plywood.payroll.service;

import com.plywood.payroll.dto.request.DepartmentRequest;
import com.plywood.payroll.dto.response.DepartmentResponse;
import com.plywood.payroll.entity.Department;
import com.plywood.payroll.exception.ResourceNotFoundException;
import com.plywood.payroll.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final com.plywood.payroll.repository.TeamRepository teamRepository;

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
            java.util.List<com.plywood.payroll.entity.Team> teams = teamRepository.findAll().stream()
                .filter(t -> t.getDepartment() != null && t.getDepartment().getId().equals(entity.getId()))
                .collect(java.util.stream.Collectors.toList());
            response.setTeamCount(teams.size());
            response.setTeamNames(teams.stream().map(com.plywood.payroll.entity.Team::getName).collect(java.util.stream.Collectors.toList()));
        }

        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
