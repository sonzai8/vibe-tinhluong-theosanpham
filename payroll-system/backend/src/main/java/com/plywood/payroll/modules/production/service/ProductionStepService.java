package com.plywood.payroll.modules.production.service;

import com.plywood.payroll.modules.production.dto.request.ProductionStepRequest;
import com.plywood.payroll.modules.production.dto.response.ProductionStepResponse;
import com.plywood.payroll.modules.production.entity.ProductionStep;
import com.plywood.payroll.shared.exception.ResourceNotFoundException;
import com.plywood.payroll.modules.production.repository.ProductionStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductionStepService {

    private final ProductionStepRepository productionStepRepository;

    public List<ProductionStepResponse> getAll() {
        return productionStepRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ProductionStepResponse getById(Long id) {
        return productionStepRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Công đoạn sản xuất", id));
    }

    @Transactional
    public ProductionStepResponse create(ProductionStepRequest request) {
        ProductionStep step = new ProductionStep();
        step.setName(request.getName());
        step.setDescription(request.getDescription());
        return mapToResponse(productionStepRepository.save(step));
    }

    @Transactional
    public ProductionStepResponse update(Long id, ProductionStepRequest request) {
        ProductionStep step = productionStepRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Công đoạn sản xuất", id));
        step.setName(request.getName());
        step.setDescription(request.getDescription());
        return mapToResponse(productionStepRepository.save(step));
    }

    @Transactional
    public void delete(Long id) {
        if (!productionStepRepository.existsById(id)) {
            throw new ResourceNotFoundException("Công đoạn sản xuất", id);
        }
        productionStepRepository.deleteById(id);
    }

    public ProductionStepResponse mapToResponse(ProductionStep entity) {
        if (entity == null) return null;
        ProductionStepResponse response = new ProductionStepResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
