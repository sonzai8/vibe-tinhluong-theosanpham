package com.plywood.payroll.modules.production.repository;

import com.plywood.payroll.modules.production.entity.StageProductMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StageProductMappingRepository extends JpaRepository<StageProductMapping, Long> {

    @Query("SELECT m.product FROM StageProductMapping m WHERE m.productionStep.id = :stepId")
    List<com.plywood.payroll.modules.product.entity.Product> findProductsByStepId(@Param("stepId") Long stepId);

    boolean existsByProductionStepIdAndProductId(Long stepId, Long productId);

    void deleteByProductionStepIdAndProductId(Long stepId, Long productId);
}
