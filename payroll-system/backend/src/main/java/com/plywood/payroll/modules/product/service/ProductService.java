package com.plywood.payroll.modules.product.service;

import com.plywood.payroll.modules.product.dto.request.ProductRequest;
import com.plywood.payroll.modules.product.dto.response.ProductResponse;
import com.plywood.payroll.modules.product.entity.Product;
import com.plywood.payroll.modules.product.entity.ProductUnit;
import com.plywood.payroll.shared.exception.ResourceNotFoundException;
import com.plywood.payroll.modules.product.repository.ProductRepository;
import com.plywood.payroll.modules.product.repository.ProductUnitRepository;
import com.plywood.payroll.modules.product.dto.response.ProductUnitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductUnitRepository productUnitRepository;

    public List<ProductResponse> getAll() {
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ProductResponse getById(Long id) {
        return productRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        mapRequestToEntity(request, product);
        return mapToResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", id));
        mapRequestToEntity(request, product);
        return mapToResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Sản phẩm", id);
        }
        productRepository.deleteById(id);
    }

    private void mapRequestToEntity(ProductRequest request, Product product) {
        product.setName(request.getName());
        product.setCode(request.getCode());
        product.setThickness(request.getThickness());
        product.setLength(request.getLength());
        product.setWidth(request.getWidth());
        product.setFilmCoatingType(request.getFilmCoatingType());
        
        ProductUnit unit = productUnitRepository.findById(request.getUnitId())
                .orElseThrow(() -> new ResourceNotFoundException("Đơn vị sản phẩm", request.getUnitId()));
        product.setUnit(unit);
    }

    public ProductResponse mapToResponse(Product entity) {
        if (entity == null) return null;
        ProductResponse response = new ProductResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setCode(entity.getCode());
        response.setThickness(entity.getThickness());
        response.setLength(entity.getLength());
        response.setWidth(entity.getWidth());
        response.setFilmCoatingType(entity.getFilmCoatingType());
        
        if (entity.getUnit() != null) {
            ProductUnitResponse unitResponse = new ProductUnitResponse();
            unitResponse.setId(entity.getUnit().getId());
            unitResponse.setName(entity.getUnit().getName());
            unitResponse.setCreatedAt(entity.getUnit().getCreatedAt());
            unitResponse.setUpdatedAt(entity.getUnit().getUpdatedAt());
            response.setUnit(unitResponse);
        }
        
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
