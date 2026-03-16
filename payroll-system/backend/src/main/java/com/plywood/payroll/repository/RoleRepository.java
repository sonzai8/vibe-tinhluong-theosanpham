package com.plywood.payroll.repository;

import com.plywood.payroll.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
}
