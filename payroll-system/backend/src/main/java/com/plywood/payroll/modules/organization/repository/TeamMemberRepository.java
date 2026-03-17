package com.plywood.payroll.modules.organization.repository;

import com.plywood.payroll.modules.organization.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
}
