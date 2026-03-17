package com.plywood.payroll.modules.organization.service;

import com.plywood.payroll.modules.organization.dto.request.RoleRequest;
import com.plywood.payroll.modules.organization.dto.response.RoleResponse;
import com.plywood.payroll.modules.organization.entity.Role;
import com.plywood.payroll.shared.exception.ResourceNotFoundException;
import com.plywood.payroll.modules.organization.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    public List<RoleResponse> getAll() {
        return roleRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public RoleResponse getById(Long id) {
        return roleRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Chức vụ", id));
    }

    @Transactional
    public RoleResponse create(RoleRequest request) {
        Role role = new Role();
        role.setName(request.getName());
        role.setDailyBenefit(request.getDailyBenefit());
        role.setPermissions(request.getPermissions() != null ? new java.util.HashSet<>(request.getPermissions()) : new java.util.HashSet<>());
        return mapToResponse(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse update(Long id, RoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chức vụ", id));
        role.setName(request.getName());
        role.setDailyBenefit(request.getDailyBenefit());
        if (request.getPermissions() != null) {
            role.setPermissions(new java.util.HashSet<>(request.getPermissions()));
        }
        return mapToResponse(roleRepository.save(role));
    }

    @Transactional
    public void delete(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Chức vụ", id);
        }
        roleRepository.deleteById(id);
    }

    public RoleResponse mapToResponse(Role entity) {
        if (entity == null) return null;
        RoleResponse response = new RoleResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDailyBenefit(entity.getDailyBenefit());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setPermissions(entity.getPermissions());
        return response;
    }
}
