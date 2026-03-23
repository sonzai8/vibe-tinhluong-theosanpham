package com.plywood.payroll.modules.quality.entity;
import com.plywood.payroll.shared.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@Entity
@Table(name = "product_qualities")
public class ProductQuality extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String code; // A, 2BA, 2B3CA

    private String description;
    
    @Column(nullable = false)
    private Integer priority = 0;

    @OneToMany(mappedBy = "quality", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ProductQualityLayer> layers = new ArrayList<>();
}
