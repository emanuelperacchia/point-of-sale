package com.pos.system.repository.system;

import com.pos.system.entity.system.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Permission findByName(Permission.PermissionName name);
}
