package com.pos.system.repository;

import com.pos.system.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByKeyHashAndActivoTrue(String keyHash);

    boolean existsByNombreAndActivoTrue(String nombre);
}
