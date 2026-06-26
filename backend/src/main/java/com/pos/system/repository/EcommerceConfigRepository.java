package com.pos.system.repository;

import com.pos.system.entity.EcommerceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EcommerceConfigRepository extends JpaRepository<EcommerceConfig, Long> {

    List<EcommerceConfig> findByActivoTrue();
}
