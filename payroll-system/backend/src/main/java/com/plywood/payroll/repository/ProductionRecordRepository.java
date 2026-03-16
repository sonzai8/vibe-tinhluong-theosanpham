package com.plywood.payroll.repository;

import com.plywood.payroll.entity.ProductionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ProductionRecordRepository extends JpaRepository<ProductionRecord, Long> {
    List<ProductionRecord> findByProductionDateBetween(LocalDate from, LocalDate to);

    @Query("SELECT r FROM ProductionRecord r WHERE FUNCTION('MONTH', r.productionDate) = :month AND FUNCTION('YEAR', r.productionDate) = :year")
    List<ProductionRecord> findByMonthAndYear(@Param("month") int month, @Param("year") int year);
}
