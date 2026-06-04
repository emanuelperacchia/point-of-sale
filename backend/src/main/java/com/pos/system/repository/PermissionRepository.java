package com.pos.system.repository;

import com.pos.system.entity.Permission;
import com.pos.system.entity.Permission.PermissionName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByName(PermissionName name);
    boolean existsByName(PermissionName name);
}
