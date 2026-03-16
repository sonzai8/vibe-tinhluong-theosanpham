package com.plywood.payroll.service;

import com.plywood.payroll.dto.request.QualityLayerSurchargeRequest;
import com.plywood.payroll.dto.response.QualityLayerSurchargeResponse;
import com.plywood.payroll.entity.QualityLayerSurcharge;
import com.plywood.payroll.exception.ResourceNotFoundException;
import com.plywood.payroll.repository.QualityLayerSurchargeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QualityLayerSurchargeService {

    private final QualityLayerSurchargeRepository surchargeRepository;

    public List<QualityLayerSurchargeResponse> getAll() {
        return surchargeRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public QualityLayerSurchargeResponse getById(Long id) {
        return surchargeRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Mức phạt lớp lỗi", id));
    }

    @Transactional
    public QualityLayerSurchargeResponse create(QualityLayerSurchargeRequest request) {
        QualityLayerSurcharge surcharge = new QualityLayerSurcharge();
        surcharge.setLayerType(request.getLayerType());
        surcharge.setSurchargePerLayer(request.getSurchargePerLayer());
        return mapToResponse(surchargeRepository.save(surcharge));
    }

    @Transactional
    public QualityLayerSurchargeResponse update(Long id, QualityLayerSurchargeRequest request) {
        QualityLayerSurcharge surcharge = surchargeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mức phạt lớp lỗi", id));
        surcharge.setLayerType(request.getLayerType());
        surcharge.setSurchargePerLayer(request.getSurchargePerLayer());
        return mapToResponse(surchargeRepository.save(surcharge));
    }

    @Transactional
    public void delete(Long id) {
        if (!surchargeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Mức phạt lớp lỗi", id);
        }
        surchargeRepository.deleteById(id);
    }

    public QualityLayerSurchargeResponse mapToResponse(QualityLayerSurcharge entity) {
        if (entity == null) return null;
        QualityLayerSurchargeResponse response = new QualityLayerSurchargeResponse();
        response.setId(entity.getId());
        response.setLayerType(entity.getLayerType());
        response.setSurchargePerLayer(entity.getSurchargePerLayer());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
