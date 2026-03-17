package com.plywood.payroll.modules.organization.repository;

import com.plywood.payroll.modules.organization.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}
