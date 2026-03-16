package com.plywood.payroll.repository;

import com.plywood.payroll.entity.ProductStepRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface ProductStepRateRepository extends JpaRepository<ProductStepRate, Long> {

    // Lấy đơn giá hiệu lực gần nhất trước hoặc bằng ngày sản xuất
    @Query("SELECT r FROM ProductStepRate r WHERE r.product.id = :productId AND r.productionStep.id = :stepId AND r.quality.id = :qualityId AND r.effectiveDate <= :date ORDER BY r.effectiveDate DESC LIMIT 1")
    Optional<ProductStepRate> findEffectiveRate(@Param("productId") Long productId,
                                                @Param("stepId") Long stepId,
                                                @Param("qualityId") Long qualityId,
                                                @Param("date") LocalDate date);
                                                
    Optional<ProductStepRate> findByProductIdAndProductionStepIdAndQualityIdAndEffectiveDate(Long productId, Long stepId, Long qualityId, LocalDate date);
}
