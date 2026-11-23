package com.pos.system.repository.system;

import com.pos.system.entity.system.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long>, JpaSpecificationExecutor<Permission> {

    Optional<Permission> findByName(Permission.PermissionName name);

    List<Permission> findByNameIn(List<Permission.PermissionName> names);

    boolean existsByName(Permission.PermissionName name);
}
