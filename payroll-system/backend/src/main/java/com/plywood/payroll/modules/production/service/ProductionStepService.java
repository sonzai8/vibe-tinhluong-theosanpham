package com.plywood.payroll.modules.production.service;

import com.plywood.payroll.modules.production.dto.request.ProductionStepRequest;
import com.plywood.payroll.modules.production.dto.response.ProductionStepResponse;
import com.plywood.payroll.modules.production.entity.ProductionStep;
import com.plywood.payroll.modules.production.entity.StageProductMapping;
import com.plywood.payroll.shared.exception.ResourceNotFoundException;
import com.plywood.payroll.modules.production.repository.ProductionStepRepository;
import com.plywood.payroll.modules.production.repository.StageProductMappingRepository;
import com.plywood.payroll.modules.product.service.ProductService;
import com.plywood.payroll.modules.product.dto.response.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductionStepService {

    private final ProductionStepRepository productionStepRepository;
    private final StageProductMappingRepository mappingRepository;
    private final ProductService productService;

    public List<ProductResponse> getProductsByStepId(Long stepId) {
        if (!productionStepRepository.existsById(stepId)) {
            throw new ResourceNotFoundException("Công đoạn sản xuất", stepId);
        }
        return mappingRepository.findProductsByStepId(stepId).stream()
                .map(productService::mapToResponse)
                .collect(Collectors.toList());
    }

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
        if (request.getPriceCalculationType() != null) {
            step.setPriceCalculationType(request.getPriceCalculationType());
        }
        ProductionStep savedStep = productionStepRepository.save(step);
        
        if (request.getProductIds() != null && !request.getProductIds().isEmpty()) {
            addProductsToStep(savedStep.getId(), request.getProductIds());
        }
        
        return mapToResponse(savedStep);
    }

    @Transactional
    public ProductionStepResponse update(Long id, ProductionStepRequest request) {
        ProductionStep step = productionStepRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Công đoạn sản xuất", id));
        step.setName(request.getName());
        step.setDescription(request.getDescription());
        if (request.getPriceCalculationType() != null) {
            step.setPriceCalculationType(request.getPriceCalculationType());
        }
        ProductionStep savedStep = productionStepRepository.save(step);
        
        if (request.getProductIds() != null) {
            // First clear existing and then add new or sync
            mappingRepository.deleteByProductionStepId(id);
            addProductsToStep(id, request.getProductIds());
        }
        
        return mapToResponse(savedStep);
    }

    @Transactional
    public void delete(Long id) {
        if (!productionStepRepository.existsById(id)) {
            throw new ResourceNotFoundException("Công đoạn sản xuất", id);
        }
        productionStepRepository.deleteById(id);
    }

    @Transactional
    public void addProductsToStep(Long stepId, List<Long> productIds) {
        ProductionStep step = productionStepRepository.findById(stepId)
                .orElseThrow(() -> new ResourceNotFoundException("Công đoạn sản xuất", stepId));
        
        for (Long productId : productIds) {
            if (!mappingRepository.existsByProductionStepIdAndProductId(stepId, productId)) {
                com.plywood.payroll.modules.product.entity.Product product = productService.getByIdRaw(productId);
                StageProductMapping mapping = new StageProductMapping();
                mapping.setProductionStep(step);
                mapping.setProduct(product);
                mappingRepository.save(mapping);
            }
        }
    }

    @Transactional
    public void removeProductFromStep(Long stepId, Long productId) {
        mappingRepository.deleteByProductionStepIdAndProductId(stepId, productId);
    }

    public ProductionStepResponse mapToResponse(ProductionStep entity) {
        if (entity == null) return null;
        ProductionStepResponse response = new ProductionStepResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setPriceCalculationType(entity.getPriceCalculationType());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        
        // Load products
        response.setProducts(mappingRepository.findProductsByStepId(entity.getId()).stream()
                .map(productService::mapToResponse)
                .collect(Collectors.toList()));
                
        return response;
    }
}
