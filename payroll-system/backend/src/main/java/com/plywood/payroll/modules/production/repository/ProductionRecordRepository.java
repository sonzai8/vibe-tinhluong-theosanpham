package com.plywood.payroll.modules.production.repository;

import com.plywood.payroll.modules.production.entity.ProductionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductionRecordRepository extends JpaRepository<ProductionRecord, Long>, JpaSpecificationExecutor<ProductionRecord> {
    List<ProductionRecord> findTop5ByOrderByCreatedAtDesc();

    @Query("SELECT r FROM ProductionRecord r WHERE EXTRACT(MONTH FROM r.productionDate) = :month AND EXTRACT(YEAR FROM r.productionDate) = :year")
    List<ProductionRecord> findByMonthAndYear(@Param("month") int month, @Param("year") int year);

    long countByProductionDate(LocalDate date);

    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM ProductionRecord r WHERE r.productionDate = :date")
    Long sumQuantityByProductionDate(@Param("date") LocalDate date);

    List<ProductionRecord> findByProductionDateBetween(LocalDate start, LocalDate end);
}
