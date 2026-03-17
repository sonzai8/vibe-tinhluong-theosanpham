package com.plywood.payroll.modules.quality.service;

import com.plywood.payroll.modules.quality.dto.request.ProductQualityRequest;
import com.plywood.payroll.modules.quality.dto.response.ProductQualityResponse;
import com.plywood.payroll.modules.quality.entity.ProductQuality;
import com.plywood.payroll.modules.quality.entity.ProductQualityLayer;
import com.plywood.payroll.modules.quality.entity.QualityLayerSurcharge;
import com.plywood.payroll.shared.exception.ResourceNotFoundException;
import com.plywood.payroll.modules.quality.repository.ProductQualityRepository;
import com.plywood.payroll.modules.quality.repository.QualityLayerSurchargeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductQualityService {

    private final ProductQualityRepository productQualityRepository;
    private final QualityLayerSurchargeRepository surchargeRepository;
    private final QualityLayerSurchargeService surchargeService;

    public List<ProductQualityResponse> getAll() {
        return productQualityRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ProductQualityResponse getById(Long id) {
        return productQualityRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Chất lượng sản phẩm", id));
    }

    @Transactional
    public ProductQualityResponse create(ProductQualityRequest request) {
        ProductQuality quality = new ProductQuality();
        quality.setCode(request.getCode());
        quality.setDescription(request.getDescription());
        
        updateLayers(quality, request.getLayers());
        
        return mapToResponse(productQualityRepository.save(quality));
    }

    @Transactional
    public ProductQualityResponse update(Long id, ProductQualityRequest request) {
        ProductQuality quality = productQualityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chất lượng sản phẩm", id));
                
        quality.setCode(request.getCode());
        quality.setDescription(request.getDescription());
        
        updateLayers(quality, request.getLayers());
        
        return mapToResponse(productQualityRepository.save(quality));
    }
    
    private void updateLayers(ProductQuality quality, List<ProductQualityRequest.QualityLayerRequest> layerRequests) {
        quality.getLayers().clear();
        
        if (layerRequests != null) {
            for (ProductQualityRequest.QualityLayerRequest lr : layerRequests) {
                QualityLayerSurcharge surcharge = surchargeRepository.findById(lr.getLayerId())
                        .orElseThrow(() -> new ResourceNotFoundException("Mức phạt", lr.getLayerId()));
                        
                ProductQualityLayer pql = new ProductQualityLayer();
                pql.setQuality(quality);
                pql.setLayer(surcharge);
                pql.setQuantity(lr.getQuantity());
                
                quality.getLayers().add(pql);
            }
        }
    }

    @Transactional
    public void delete(Long id) {
        if (!productQualityRepository.existsById(id)) {
            throw new ResourceNotFoundException("Chất lượng sản phẩm", id);
        }
        productQualityRepository.deleteById(id);
    }

    public ProductQualityResponse mapToResponse(ProductQuality entity) {
        if (entity == null) return null;
        ProductQualityResponse response = new ProductQualityResponse();
        response.setId(entity.getId());
        response.setCode(entity.getCode());
        response.setDescription(entity.getDescription());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        
        if (entity.getLayers() != null) {
            List<ProductQualityResponse.QualityLayerResponse> layerResponses = entity.getLayers().stream()
                    .map(layer -> {
                        ProductQualityResponse.QualityLayerResponse lr = new ProductQualityResponse.QualityLayerResponse();
                        lr.setId(layer.getId());
                        lr.setLayer(surchargeService.mapToResponse(layer.getLayer()));
                        lr.setQuantity(layer.getQuantity());
                        return lr;
                    })
                    .collect(Collectors.toList());
            response.setLayers(layerResponses);
        } else {
            response.setLayers(new ArrayList<>());
        }
        
        return response;
    }
}
