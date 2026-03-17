package com.plywood.payroll.modules.product.service;

import com.plywood.payroll.modules.product.dto.request.ProductUnitRequest;
import com.plywood.payroll.modules.product.dto.response.ProductUnitResponse;
import com.plywood.payroll.modules.product.entity.ProductUnit;
import com.plywood.payroll.shared.exception.BusinessException;
import com.plywood.payroll.shared.exception.ResourceNotFoundException;
import com.plywood.payroll.modules.product.repository.ProductUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductUnitService {

    private final ProductUnitRepository productUnitRepository;

    public List<ProductUnitResponse> getAll() {
        return productUnitRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ProductUnitResponse getById(Long id) {
        return productUnitRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn vị sản phẩm", id));
    }

    @Transactional
    public ProductUnitResponse create(ProductUnitRequest request) {
        if (productUnitRepository.findByName(request.getName()).isPresent()) {
            throw new BusinessException("Tên đơn vị sản phẩm đã tồn tại");
        }
        ProductUnit unit = new ProductUnit();
        unit.setName(request.getName());
        return mapToResponse(productUnitRepository.save(unit));
    }

    @Transactional
    public ProductUnitResponse update(Long id, ProductUnitRequest request) {
        ProductUnit unit = productUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn vị sản phẩm", id));
                
        if (!unit.getName().equals(request.getName()) && 
            productUnitRepository.findByName(request.getName()).isPresent()) {
            throw new BusinessException("Tên đơn vị sản phẩm đã tồn tại");
        }
        
        unit.setName(request.getName());
        return mapToResponse(productUnitRepository.save(unit));
    }

    @Transactional
    public void delete(Long id) {
        if (!productUnitRepository.existsById(id)) {
            throw new ResourceNotFoundException("Đơn vị sản phẩm", id);
        }
        productUnitRepository.deleteById(id);
    }

    private ProductUnitResponse mapToResponse(ProductUnit entity) {
        if (entity == null) return null;
        ProductUnitResponse response = new ProductUnitResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
