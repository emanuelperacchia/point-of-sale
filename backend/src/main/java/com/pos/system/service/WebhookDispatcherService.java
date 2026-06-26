package com.pos.system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.system.entity.WebhookDelivery;
import com.pos.system.entity.WebhookEndpoint;
import com.pos.system.repository.WebhookDeliveryRepository;
import com.pos.system.repository.WebhookEndpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Despacha webhooks a endpoints externos de forma asíncrona.
 * Firma los payloads con HMAC-SHA256 usando el secreto del webhook.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookDispatcherService {

    private final WebhookEndpointRepository endpointRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final ObjectMapper objectMapper;

    private static final int MAX_RETRIES = 3;
    private static final int TIMEOUT_SECONDS = 10;
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .build();

    /**
     * Despacha un evento a todos los webhooks suscritos.
     * Se llama desde los servicios (ej. SaleService.processSale()).
     */
    @Async
    public void dispatch(String evento, Object payload) {
        List<WebhookEndpoint> suscritos = endpointRepository.findActivosByEvento(evento);
        if (suscritos.isEmpty()) return;

        String payloadJson;
        try {
            Map<String, Object> envelope = Map.of(
                    "evento", evento,
                    "timestamp", LocalDateTime.now().toString(),
                    "data", payload
            );
            payloadJson = objectMapper.writeValueAsString(envelope);
        } catch (Exception e) {
            log.error("Error serializando payload para webhook {}", evento, e);
            return;
        }

        for (WebhookEndpoint wh : suscritos) {
            WebhookDelivery delivery = WebhookDelivery.builder()
                    .webhookId(wh.getId())
                    .evento(evento)
                    .payload(payloadJson)
                    .estado(WebhookDelivery.Estado.PENDIENTE)
                    .build();
            delivery = deliveryRepository.save(delivery);
            sendDelivery(wh, delivery, payloadJson);
        }
    }

    /**
     * Envía un delivery individual con firma HMAC.
     */
    private void sendDelivery(WebhookEndpoint wh, WebhookDelivery delivery, String payloadJson) {
        try {
            String signature = signPayload(payloadJson, wh.getSecreto());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(wh.getUrl()))
                    .header("Content-Type", "application/json")
                    .header("X-POS-Signature", "sha256=" + signature)
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            delivery.setStatusCode(statusCode);
            delivery.setIntentos(delivery.getIntentos() + 1);
            delivery.setUltimoIntento(LocalDateTime.now());

            if (statusCode >= 200 && statusCode < 300) {
                delivery.setEstado(WebhookDelivery.Estado.ENTREGADO);
                log.info("Webhook {} entregado a {} con status {}", delivery.getEvento(), wh.getUrl(), statusCode);
            } else {
                delivery.setEstado(delivery.getIntentos() >= MAX_RETRIES
                        ? WebhookDelivery.Estado.FALLIDO
                        : WebhookDelivery.Estado.PENDIENTE);
                log.warn("Webhook {} falló en {}: HTTP {}", delivery.getEvento(), wh.getUrl(), statusCode);
            }

        } catch (Exception e) {
            delivery.setIntentos(delivery.getIntentos() + 1);
            delivery.setUltimoIntento(LocalDateTime.now());
            delivery.setEstado(delivery.getIntentos() >= MAX_RETRIES
                    ? WebhookDelivery.Estado.FALLIDO
                    : WebhookDelivery.Estado.PENDIENTE);
            log.warn("Webhook {} error enviando a {}: {}", delivery.getEvento(), wh.getUrl(), e.getMessage());
        }

        deliveryRepository.save(delivery);
    }

    /**
     * Firma el payload con HMAC-SHA256 usando el secreto del webhook.
     */
    private String signPayload(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error firmando payload", e);
        }
    }

    /**
     * Scheduler: reintenta deliveries PENDIENTES con < 3 intentos cada 5 minutos.
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void retryPending() {
        List<WebhookDelivery> pendientes = deliveryRepository
                .findByEstadoAndIntentosLessThan(WebhookDelivery.Estado.PENDIENTE, MAX_RETRIES);

        for (WebhookDelivery delivery : pendientes) {
            endpointRepository.findById(delivery.getWebhookId()).ifPresent(wh -> {
                if (!wh.getActivo()) {
                    delivery.setEstado(WebhookDelivery.Estado.FALLIDO);
                    deliveryRepository.save(delivery);
                    return;
                }
                // Backoff exponencial: 30s, 1min, 2min...
                long backoffSeconds = (long) Math.pow(2, delivery.getIntentos()) * 15;
                if (delivery.getUltimoIntento() != null &&
                        delivery.getUltimoIntento().plusSeconds(backoffSeconds).isAfter(LocalDateTime.now())) {
                    return; // aún no es tiempo de reintentar
                }
                sendDelivery(wh, delivery, delivery.getPayload());
            });
        }

        if (!pendientes.isEmpty()) {
            log.info("Reintentando {} webhooks pendientes", pendientes.size());
        }
    }
}
