package com.plywood.payroll.repository;

import com.plywood.payroll.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
}
