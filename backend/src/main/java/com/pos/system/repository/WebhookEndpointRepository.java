package com.pos.system.repository;

import com.pos.system.entity.WebhookEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, Long> {

    List<WebhookEndpoint> findByActivoTrue();

    @Query("SELECT w FROM WebhookEndpoint w WHERE w.activo = true AND w.eventos LIKE %:evento%")
    List<WebhookEndpoint> findActivosByEvento(@Param("evento") String evento);
}
