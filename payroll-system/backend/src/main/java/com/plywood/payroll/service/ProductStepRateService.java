package com.plywood.payroll.service;

import com.plywood.payroll.dto.request.ProductStepRateRequest;
import com.plywood.payroll.dto.response.ProductStepRateResponse;
import com.plywood.payroll.entity.Product;
import com.plywood.payroll.entity.ProductStepRate;
import com.plywood.payroll.entity.ProductionStep;
import com.plywood.payroll.exception.ResourceNotFoundException;
import com.plywood.payroll.repository.ProductRepository;
import com.plywood.payroll.repository.ProductStepRateRepository;
import com.plywood.payroll.repository.ProductionStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductStepRateService {

    private final ProductStepRateRepository productStepRateRepository;
    private final ProductRepository productRepository;
    private final ProductionStepRepository productionStepRepository;
    private final ProductService productService;
    private final ProductionStepService productionStepService;

    public List<ProductStepRateResponse> getAll() {
        return productStepRateRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ProductStepRateResponse getById(Long id) {
        return productStepRateRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn giá", id));
    }

    @Transactional
    public ProductStepRateResponse create(ProductStepRateRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", request.getProductId()));
                
        ProductionStep step = productionStepRepository.findById(request.getProductionStepId())
                .orElseThrow(() -> new ResourceNotFoundException("Công đoạn", request.getProductionStepId()));

        ProductStepRate rate = new ProductStepRate();
        rate.setProduct(product);
        rate.setProductionStep(step);
        rate.setBasePrice(request.getBasePrice());
        rate.setEffectiveDate(request.getEffectiveDate());

        return mapToResponse(productStepRateRepository.save(rate));
    }

    @Transactional
    public ProductStepRateResponse update(Long id, ProductStepRateRequest request) {
        ProductStepRate rate = productStepRateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn giá", id));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", request.getProductId()));
                
        ProductionStep step = productionStepRepository.findById(request.getProductionStepId())
                .orElseThrow(() -> new ResourceNotFoundException("Công đoạn", request.getProductionStepId()));

        rate.setProduct(product);
        rate.setProductionStep(step);
        rate.setBasePrice(request.getBasePrice());
        rate.setEffectiveDate(request.getEffectiveDate());

        return mapToResponse(productStepRateRepository.save(rate));
    }

    @Transactional
    public void delete(Long id) {
        if (!productStepRateRepository.existsById(id)) {
            throw new ResourceNotFoundException("Đơn giá", id);
        }
        productStepRateRepository.deleteById(id);
    }

    public ProductStepRateResponse mapToResponse(ProductStepRate entity) {
        if (entity == null) return null;
        ProductStepRateResponse response = new ProductStepRateResponse();
        response.setId(entity.getId());
        response.setProduct(productService.mapToResponse(entity.getProduct()));
        response.setProductionStep(productionStepService.mapToResponse(entity.getProductionStep()));
        response.setBasePrice(entity.getBasePrice());
        response.setEffectiveDate(entity.getEffectiveDate());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
