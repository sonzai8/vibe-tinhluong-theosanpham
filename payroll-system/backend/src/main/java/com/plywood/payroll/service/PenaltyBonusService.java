package com.plywood.payroll.service;

import com.plywood.payroll.dto.request.PenaltyBonusRequest;
import com.plywood.payroll.dto.response.PenaltyBonusResponse;
import com.plywood.payroll.entity.Employee;
import com.plywood.payroll.entity.PenaltyBonus;
import com.plywood.payroll.exception.ResourceNotFoundException;
import com.plywood.payroll.repository.EmployeeRepository;
import com.plywood.payroll.repository.PenaltyBonusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PenaltyBonusService {

    private final PenaltyBonusRepository penaltyBonusRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;

    public List<PenaltyBonusResponse> getAll() {
        return penaltyBonusRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<PenaltyBonusResponse> getByEmployee(Long employeeId) {
        return penaltyBonusRepository.findByEmployeeId(employeeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PenaltyBonusResponse create(PenaltyBonusRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên", request.getEmployeeId()));

        PenaltyBonus pb = new PenaltyBonus();
        pb.setEmployee(employee);
        pb.setRecordDate(request.getRecordDate());
        pb.setAmount(request.getAmount());
        pb.setReason(request.getReason());

        return mapToResponse(penaltyBonusRepository.save(pb));
    }

    @Transactional
    public PenaltyBonusResponse update(Long id, PenaltyBonusRequest request) {
        PenaltyBonus pb = penaltyBonusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khen thưởng/Kỷ luật", id));

        pb.setRecordDate(request.getRecordDate());
        pb.setAmount(request.getAmount());
        pb.setReason(request.getReason());

        return mapToResponse(penaltyBonusRepository.save(pb));
    }

    @Transactional
    public void delete(Long id) {
        if (!penaltyBonusRepository.existsById(id)) {
            throw new ResourceNotFoundException("Khen thưởng/Kỷ luật", id);
        }
        penaltyBonusRepository.deleteById(id);
    }

    public PenaltyBonusResponse mapToResponse(PenaltyBonus entity) {
        if (entity == null) return null;
        PenaltyBonusResponse response = new PenaltyBonusResponse();
        response.setId(entity.getId());
        response.setEmployee(employeeService.mapToResponse(entity.getEmployee()));
        response.setRecordDate(entity.getRecordDate());
        response.setAmount(entity.getAmount());
        response.setReason(entity.getReason());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
