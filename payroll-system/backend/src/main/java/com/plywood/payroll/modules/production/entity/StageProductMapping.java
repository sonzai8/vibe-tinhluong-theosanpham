package com.plywood.payroll.modules.production.entity;

import com.plywood.payroll.modules.product.entity.Product;
import com.plywood.payroll.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "stage_product_mappings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"production_step_id", "product_id"})
})
public class StageProductMapping extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "production_step_id")
    private ProductionStep productionStep;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;
}
