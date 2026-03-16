package com.plywood.payroll.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
@Entity
@Table(name = "product_step_rates")
public class ProductStepRate extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(optional = false)
    @JoinColumn(name = "production_step_id")
    private ProductionStep productionStep;

    @Column(nullable = false)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private LocalDate effectiveDate;
}
