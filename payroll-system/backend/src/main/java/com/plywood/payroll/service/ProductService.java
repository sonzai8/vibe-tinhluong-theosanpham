package com.plywood.payroll.service;

import com.plywood.payroll.dto.request.ProductRequest;
import com.plywood.payroll.dto.response.ProductResponse;
import com.plywood.payroll.entity.Product;
import com.plywood.payroll.exception.ResourceNotFoundException;
import com.plywood.payroll.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

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
        product.setCode(request.getCode());
        product.setThickness(request.getThickness());
        product.setLength(request.getLength());
        product.setWidth(request.getWidth());
    }

    public ProductResponse mapToResponse(Product entity) {
        if (entity == null) return null;
        ProductResponse response = new ProductResponse();
        response.setId(entity.getId());
        response.setCode(entity.getCode());
        response.setThickness(entity.getThickness());
        response.setLength(entity.getLength());
        response.setWidth(entity.getWidth());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
