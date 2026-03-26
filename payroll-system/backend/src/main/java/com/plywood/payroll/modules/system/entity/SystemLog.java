package com.plywood.payroll.modules.system.entity;

import com.plywood.payroll.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "system_logs")
public class SystemLog extends BaseEntity {
    
    @Column(nullable = false)
    private String type; // e.g., "ZKTECO_MAPPING_ERROR", "SYSTEM_ERROR"
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;
    
    @Column(columnDefinition = "TEXT")
    private String detail;
    
    private boolean isResolved = false;
    
    @Column(columnDefinition = "TEXT")
    private String resolutionNote;
}
