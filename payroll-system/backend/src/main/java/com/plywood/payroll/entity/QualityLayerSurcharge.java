package com.plywood.payroll.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
@Entity
@Table(name = "quality_layer_surcharges")
public class QualityLayerSurcharge extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String layerType; // "Lớp B", "Lớp C"

    @Column(nullable = false)
    private BigDecimal surchargePerLayer = BigDecimal.ZERO;
}
