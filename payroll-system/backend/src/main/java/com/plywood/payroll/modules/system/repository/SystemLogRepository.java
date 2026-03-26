package com.plywood.payroll.modules.system.repository;

import com.plywood.payroll.modules.system.entity.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {
    List<SystemLog> findByIsResolved(boolean isResolved);
}
