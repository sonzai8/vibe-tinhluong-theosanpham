package com.plywood.payroll.modules.production.service;

import com.plywood.payroll.modules.organization.entity.Team;
import com.plywood.payroll.modules.organization.repository.TeamRepository;
import com.plywood.payroll.modules.product.entity.Product;
import com.plywood.payroll.modules.product.repository.ProductRepository;
import com.plywood.payroll.modules.production.dto.request.ProductionRecordRequest;
import com.plywood.payroll.modules.production.entity.ProductionStep;
import com.plywood.payroll.modules.production.repository.ProductionRecordRepository;
import com.plywood.payroll.modules.production.repository.StageProductMappingRepository;
import com.plywood.payroll.modules.quality.entity.ProductQuality;
import com.plywood.payroll.modules.quality.repository.ProductQualityRepository;
import com.plywood.payroll.shared.exception.BusinessException;
import com.plywood.payroll.modules.organization.service.TeamService;
import com.plywood.payroll.modules.product.service.ProductService;
import com.plywood.payroll.modules.quality.service.ProductQualityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductionRecordServiceTest {

    @Mock private ProductionRecordRepository recordRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductQualityRepository qualityRepository;
    @Mock private StageProductMappingRepository mappingRepository;
    @Mock private TeamService teamService;
    @Mock private ProductService productService;
    @Mock private ProductQualityService qualityService;

    @InjectMocks
    private ProductionRecordService recordService;

    private Team team;
    private Product product;
    private ProductQuality quality;
    private ProductionStep step;

    @BeforeEach
    void setUp() {
        step = new ProductionStep();
        step.setId(1L);
        step.setName("Pressing");

        team = new Team();
        team.setId(1L);
        team.setProductionStep(step);

        product = new Product();
        product.setId(1L);
        product.setName("Plywood 18mm");

        quality = new ProductQuality();
        quality.setId(1L);
    }

    @Test
    void testCreate_Success() {
        ProductionRecordRequest request = new ProductionRecordRequest();
        request.setTeamId(1L);
        request.setProductId(1L);
        request.setQualityId(1L);
        request.setQuantity(50);
        request.setProductionDate(LocalDate.now());

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(qualityRepository.findById(1L)).thenReturn(Optional.of(quality));
        when(mappingRepository.existsByProductionStepIdAndProductId(1L, 1L)).thenReturn(true);
        when(recordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        recordService.create(request);

        verify(recordRepository, times(1)).save(any());
    }

    @Test
    void testCreate_InvalidMapping() {
        ProductionRecordRequest request = new ProductionRecordRequest();
        request.setTeamId(1L);
        request.setProductId(1L);
        request.setQualityId(1L);

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(qualityRepository.findById(1L)).thenReturn(Optional.of(quality));
        when(mappingRepository.existsByProductionStepIdAndProductId(1L, 1L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> recordService.create(request));
    }
}
