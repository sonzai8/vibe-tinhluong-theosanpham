package com.plywood.payroll.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
@Entity
@Table(name = "team_daily_funds", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"fund_date", "team_id"})
})
public class TeamDailyFund extends BaseEntity {
    @Column(nullable = false)
    private LocalDate fundDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "team_id")
    private Team team;

    // Quỹ từ sản xuất thực tế của tổ (không gồm lính đánh thuê)
    private BigDecimal ownProductionFund = BigDecimal.ZERO;

    // Quỹ từ lính đánh thuê mang về (khi thành viên đi sang tổ khác làm)
    private BigDecimal rentedOutFund = BigDecimal.ZERO;

    // Tổng quỹ = own + rented
    private BigDecimal totalFund = BigDecimal.ZERO;

    // Số thành viên biên chế tổ có đi làm (để chia đều)
    private Integer headcountToDivide = 0;
}
