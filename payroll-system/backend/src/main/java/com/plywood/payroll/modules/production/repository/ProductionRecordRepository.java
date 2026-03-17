package com.plywood.payroll.modules.production.repository;

import com.plywood.payroll.modules.production.entity.ProductionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ProductionRecordRepository extends JpaRepository<ProductionRecord, Long> {
    List<ProductionRecord> findTop5ByOrderByCreatedAtDesc();

    @Query("SELECT r FROM ProductionRecord r WHERE FUNCTION('MONTH', r.productionDate) = :month AND FUNCTION('YEAR', r.productionDate) = :year")
    List<ProductionRecord> findByMonthAndYear(@Param("month") int month, @Param("year") int year);

    long countByProductionDate(LocalDate date);

    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM ProductionRecord r WHERE r.productionDate = :date")
    int sumQuantityByProductionDate(@Param("date") LocalDate date);

    @Query("SELECT r FROM ProductionRecord r WHERE " +
           "(:from IS NULL OR r.productionDate >= :from) AND " +
           "(:to IS NULL OR r.productionDate <= :to) AND " +
           "(:departmentIds IS NULL OR r.team.department.id IN :departmentIds) AND " +
           "(:teamIds IS NULL OR r.team.id IN :teamIds) " +
           "ORDER BY r.productionDate DESC, r.createdAt DESC")
    List<ProductionRecord> findByFilters(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("departmentIds") List<Long> departmentIds,
            @Param("teamIds") List<Long> teamIds);
}
