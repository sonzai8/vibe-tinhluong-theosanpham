package com.plywood.payroll.modules.production.service;
import com.plywood.payroll.modules.quality.service.ProductQualityService;
import com.plywood.payroll.modules.organization.repository.TeamRepository;
import com.plywood.payroll.modules.production.repository.ProductionRecordRepository;
import com.plywood.payroll.modules.production.repository.StageProductMappingRepository;
import com.plywood.payroll.modules.product.repository.ProductRepository;
import com.plywood.payroll.modules.quality.repository.ProductQualityRepository;
import com.plywood.payroll.modules.product.service.ProductService;
import com.plywood.payroll.modules.organization.service.TeamService;
import com.plywood.payroll.modules.production.entity.ProductionRecord;
import com.plywood.payroll.modules.quality.entity.ProductQuality;
import com.plywood.payroll.modules.product.entity.Product;
import com.plywood.payroll.modules.organization.entity.Team;

import com.plywood.payroll.modules.production.dto.request.ProductionRecordRequest;
import com.plywood.payroll.modules.production.dto.response.ProductionRecordResponse;
// REMOVED_WILDCARD_ENTITY_IMPORT - please update manually
import com.plywood.payroll.shared.exception.ResourceNotFoundException;
// REMOVED_WILDCARD_REPO_IMPORT - please update manually
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductionRecordService {

    private final ProductionRecordRepository recordRepository;
    private final TeamRepository teamRepository;
    private final ProductRepository productRepository;
    private final ProductQualityRepository qualityRepository;
    private final StageProductMappingRepository mappingRepository;

    public List<ProductionRecordResponse> getByFilters(LocalDate from, LocalDate to, List<Long> departmentIds, List<Long> teamIds) {
        Specification<ProductionRecord> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("productionDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("productionDate"), to));
            }
            if (departmentIds != null && !departmentIds.isEmpty()) {
                predicates.add(root.get("team").get("department").get("id").in(departmentIds));
            }
            if (teamIds != null && !teamIds.isEmpty()) {
                predicates.add(root.get("team").get("id").in(teamIds));
            }

            if (query != null) {
                query.orderBy(cb.desc(root.get("productionDate")), cb.desc(root.get("createdAt")));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        List<ProductionRecord> records = recordRepository.findAll(spec);
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
                
        // Validation: Product must be mapped to team's production step
        Long stepId = team.getProductionStep().getId();
        if (!mappingRepository.existsByProductionStepIdAndProductId(stepId, request.getProductId())) {
            throw new com.plywood.payroll.shared.exception.BusinessException(
                "Sản phẩm '" + product.getName() + "' không thuộc công đoạn '" + team.getProductionStep().getName() + "'"
            );
        }
                
        record.setProductionDate(request.getProductionDate());
        record.setTeam(team);
        record.setProduct(product);
        record.setQuality(quality);
        record.setQuantity(request.getQuantity());
        
        return record;
    }

    public ProductionRecordResponse mapToResponse(ProductionRecord entity) {
        if (entity == null) return null;
        
        Team team = entity.getTeam();
        Product product = entity.getProduct();
        ProductQuality quality = entity.getQuality();
        
        return ProductionRecordResponse.builder()
                .id(entity.getId())
                .productionDate(entity.getProductionDate())
                .teamId(team != null ? team.getId() : null)
                .teamName(team != null ? team.getName() : null)
                .teamProductionStepId(team != null && team.getProductionStep() != null ? team.getProductionStep().getId() : null)
                .teamStepName(team != null && team.getProductionStep() != null ? team.getProductionStep().getName() : null)
                .productId(product != null ? product.getId() : null)
                .productCode(product != null ? product.getCode() : null)
                .productName(product != null ? product.getName() : null)
                .productThickness(product != null ? product.getThickness() : null)
                .productLength(product != null ? product.getLength() : null)
                .productWidth(product != null ? product.getWidth() : null)
                .qualityId(quality != null ? quality.getId() : null)
                .qualityCode(quality != null ? quality.getCode() : null)
                .quantity(entity.getQuantity())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

}
