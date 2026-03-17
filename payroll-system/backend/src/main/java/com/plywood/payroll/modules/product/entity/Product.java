package com.plywood.payroll.modules.product.entity;
import com.plywood.payroll.shared.entity.BaseEntity;

import com.plywood.payroll.modules.product.enums.FilmCoatingType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
@Entity
@Table(name = "products")
public class Product extends BaseEntity {
    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code; // Vd: 12LY_122x244

    @Column(nullable = false)
    private BigDecimal thickness; // Độ dày (ly)

    @Column(nullable = false)
    private BigDecimal length; // Chiều dài (m)

    @Column(nullable = false)
    private BigDecimal width; // Chiều rộng (m)

    @Enumerated(EnumType.STRING)
    @Column(name = "film_coating_type", nullable = false)
    private FilmCoatingType filmCoatingType = FilmCoatingType.NONE;

    @ManyToOne(optional = false)
    @JoinColumn(name = "unit_id")
    private ProductUnit unit;
}
