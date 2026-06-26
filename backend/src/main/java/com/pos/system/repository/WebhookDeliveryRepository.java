package com.pos.system.repository;

import com.pos.system.entity.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {

    List<WebhookDelivery> findByWebhookIdOrderByCreatedAtDesc(Long webhookId);

    List<WebhookDelivery> findByEstadoAndIntentosLessThan(WebhookDelivery.Estado estado, int intentos);
}
