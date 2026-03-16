package com.plywood.payroll.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
@Entity
@Table(name = "production_records")
public class ProductionRecord extends BaseEntity {
    @Column(nullable = false)
    private LocalDate productionDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(optional = false)
    @JoinColumn(name = "quality_id")
    private ProductQuality quality;

    @Column(nullable = false)
    private Integer quantity = 0;
}
