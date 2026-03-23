package com.plywood.payroll.modules.employee.entity;

import com.plywood.payroll.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "employee_notes")
public class EmployeeNote extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    // Ngữ cảnh tháng/năm (tùy chọn)
    private Integer month;
    private Integer year;
    
    private String createdBy;
}
