package com.plywood.payroll.modules.employee.repository;

import com.plywood.payroll.modules.employee.entity.EmployeeNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeNoteRepository extends JpaRepository<EmployeeNote, Long> {
    List<EmployeeNote> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
}
