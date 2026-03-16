package com.plywood.payroll.service;

import com.plywood.payroll.dto.request.ProductionRecordRequest;
import com.plywood.payroll.dto.response.ProductionRecordResponse;
import com.plywood.payroll.entity.*;
import com.plywood.payroll.exception.ResourceNotFoundException;
import com.plywood.payroll.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductionRecordService {

    private final ProductionRecordRepository recordRepository;
    private final TeamRepository teamRepository;
    private final ProductRepository productRepository;
    private final ProductQualityRepository qualityRepository;
    
    private final TeamService teamService;
    private final ProductService productService;
    private final ProductQualityService qualityService;

    public List<ProductionRecordResponse> getByDateRange(LocalDate from, LocalDate to) {
        List<ProductionRecord> records;
        if (from != null && to != null) {
            records = recordRepository.findByProductionDateBetween(from, to);
        } else {
            records = recordRepository.findAll();
        }
        return records.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ProductionRecordResponse getById(Long id) {
        return recordRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Bản ghi sản xuất", id));
    }

    @Transactional
    public ProductionRecordResponse create(ProductionRecordRequest request) {
        return mapToResponse(recordRepository.save(mapRequestToEntity(request, new ProductionRecord())));
    }

    @Transactional
    public ProductionRecordResponse update(Long id, ProductionRecordRequest request) {
        ProductionRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bản ghi sản xuất", id));
        return mapToResponse(recordRepository.save(mapRequestToEntity(request, record)));
    }
    
    @Transactional
    public List<ProductionRecordResponse> saveBatch(List<ProductionRecordRequest> requests) {
        List<ProductionRecord> records = requests.stream()
                .map(req -> mapRequestToEntity(req, new ProductionRecord()))
                .collect(Collectors.toList());
                
        return recordRepository.saveAll(records).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        if (!recordRepository.existsById(id)) {
            throw new ResourceNotFoundException("Bản ghi sản xuất", id);
        }
        recordRepository.deleteById(id);
    }
    
    private ProductionRecord mapRequestToEntity(ProductionRecordRequest request, ProductionRecord record) {
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Tổ sản xuất", request.getTeamId()));
                
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", request.getProductId()));
                
        ProductQuality quality = qualityRepository.findById(request.getQualityId())
                .orElseThrow(() -> new ResourceNotFoundException("Chất lượng", request.getQualityId()));
                
        record.setProductionDate(request.getProductionDate());
        record.setTeam(team);
        record.setProduct(product);
        record.setQuality(quality);
        record.setQuantity(request.getQuantity());
        
        return record;
    }

    public ProductionRecordResponse mapToResponse(ProductionRecord entity) {
        if (entity == null) return null;
        ProductionRecordResponse response = new ProductionRecordResponse();
        response.setId(entity.getId());
        response.setProductionDate(entity.getProductionDate());
        response.setTeam(teamService.mapToResponse(entity.getTeam()));
        response.setProduct(productService.mapToResponse(entity.getProduct()));
        response.setQuality(qualityService.mapToResponse(entity.getQuality()));
        response.setQuantity(entity.getQuantity());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
