package com.plywood.payroll.modules.attendance.service;

import com.plywood.payroll.modules.attendance.dto.request.AttendanceDefinitionRequest;
import com.plywood.payroll.modules.attendance.dto.response.AttendanceDefinitionResponse;
import com.plywood.payroll.modules.attendance.entity.AttendanceDefinition;
import com.plywood.payroll.modules.attendance.repository.AttendanceDefinitionRepository;
import com.plywood.payroll.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceDefinitionService {

    private final AttendanceDefinitionRepository definitionRepository;

    public List<AttendanceDefinitionResponse> getAll() {
        return definitionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public AttendanceDefinitionResponse getById(Long id) {
        return definitionRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance definition not found with id: " + id));
    }

    @Transactional
    public AttendanceDefinitionResponse create(AttendanceDefinitionRequest request) {
        AttendanceDefinition definition = new AttendanceDefinition();
        definition.setCode(request.getCode());
        definition.setName(request.getName());
        definition.setMultiplier(request.getMultiplier());
        definition.setDescription(request.getDescription());
        return mapToResponse(definitionRepository.save(definition));
    }

    @Transactional
    public AttendanceDefinitionResponse update(Long id, AttendanceDefinitionRequest request) {
        AttendanceDefinition definition = definitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance definition not found with id: " + id));
        definition.setCode(request.getCode());
        definition.setName(request.getName());
        definition.setMultiplier(request.getMultiplier());
        definition.setDescription(request.getDescription());
        return mapToResponse(definitionRepository.save(definition));
    }

    @Transactional
    public void delete(Long id) {
        if (!definitionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Attendance definition not found with id: " + id);
        }
        definitionRepository.deleteById(id);
    }

    public AttendanceDefinitionResponse mapToResponse(AttendanceDefinition definition) {
        AttendanceDefinitionResponse response = new AttendanceDefinitionResponse();
        response.setId(definition.getId());
        response.setCode(definition.getCode());
        response.setName(definition.getName());
        response.setMultiplier(definition.getMultiplier());
        response.setDescription(definition.getDescription());
        response.setCreatedAt(definition.getCreatedAt());
        response.setUpdatedAt(definition.getUpdatedAt());
        return response;
    }
}
