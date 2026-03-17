package com.plywood.payroll.modules.attendance.repository;

import com.plywood.payroll.modules.attendance.entity.AttendanceDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AttendanceDefinitionRepository extends JpaRepository<AttendanceDefinition, Long> {
    Optional<AttendanceDefinition> findByCode(String code);
}
