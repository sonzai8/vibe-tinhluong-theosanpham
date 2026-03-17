package com.plywood.payroll.modules.quality.entity;
import com.plywood.payroll.shared.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "product_quality_layers", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"quality_id", "layer_id"})
})
public class ProductQualityLayer extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "quality_id")
    private ProductQuality quality;

    @ManyToOne(optional = false)
    @JoinColumn(name = "layer_id")
    private QualityLayerSurcharge layer;

    @Column(nullable = false)
    private Integer quantity = 1; // Số lớp lỗi (vd: 2BA -> quantity=2)
}
