package com.plywood.payroll.modules.payroll.repository;

import com.plywood.payroll.modules.payroll.entity.PayrollConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PayrollConfigRepository extends JpaRepository<PayrollConfig, Long> {
    Optional<PayrollConfig> findByConfigKeyAndMonthAndYear(String configKey, Integer month, Integer year);
    
    @Query("SELECT c FROM PayrollConfig c WHERE c.configKey = :key AND (c.year < :year OR (c.year = :year AND c.month <= :month)) ORDER BY c.year DESC, c.month DESC LIMIT 1")
    Optional<PayrollConfig> findEffectiveConfig(@Param("key") String key, @Param("month") Integer month, @Param("year") Integer year);

    Optional<PayrollConfig> findByConfigKey(String configKey);
}
