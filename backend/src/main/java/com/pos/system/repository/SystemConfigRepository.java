package com.pos.system.repository;

import com.pos.system.entity.SystemConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfiguration, Long> {

    Optional<SystemConfiguration> findByConfigKey(String configKey);

    boolean existsByConfigKey(String configKey);

    List<SystemConfiguration> findByGroupName(String groupName);

    List<SystemConfiguration> findByActiveTrue();
}