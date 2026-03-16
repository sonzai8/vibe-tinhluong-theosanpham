package com.plywood.payroll.entity;

import com.plywood.payroll.enums.FilmCoatingType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
@Entity
@Table(name = "products")
public class Product extends BaseEntity {
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
}
