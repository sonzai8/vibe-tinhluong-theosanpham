package com.plywood.payroll.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
@Entity
@Table(name = "product_step_rates", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"product_id", "production_step_id", "quality_id", "effective_date"})
})
public class ProductStepRate extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(optional = false)
    @JoinColumn(name = "production_step_id")
    private ProductionStep productionStep;

    @ManyToOne(optional = false)
    @JoinColumn(name = "quality_id")
    private ProductQuality quality;

    @Column(nullable = false)
    private BigDecimal priceHigh; // Giá khi đạt chuyên cần

    @Column(nullable = false)
    private BigDecimal priceLow; // Giá khi không đạt chuyên cần hoặc mức thường

    @Column(nullable = false)
    private LocalDate effectiveDate;
}
