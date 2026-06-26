package com.pos.system.service;

import com.pos.system.dto.request.WebhookRequest;
import com.pos.system.dto.response.WebhookDeliveryResponse;
import com.pos.system.dto.response.WebhookResponse;
import com.pos.system.entity.WebhookDelivery;
import com.pos.system.entity.WebhookEndpoint;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.WebhookDeliveryRepository;
import com.pos.system.repository.WebhookEndpointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookEndpointRepository endpointRepository;
    private final WebhookDeliveryRepository deliveryRepository;

    private String generateSecret() {
        SecureRandom rng = new SecureRandom();
        byte[] bytes = new byte[16];
        rng.nextBytes(bytes);
        return "whsec_" + HexFormat.of().formatHex(bytes);
    }

    @Transactional
    public WebhookResponse create(WebhookRequest request, Long userId) {
        String secret = generateSecret();
        String eventos = request.getEventos() != null
                ? String.join(",", request.getEventos())
                : "";

        WebhookEndpoint endpoint = WebhookEndpoint.builder()
                .url(request.getUrl())
                .eventos(eventos)
                .secreto(secret)
                .activo(request.getActivo() != null ? request.getActivo() : true)
                .creadoPor(userId)
                .build();

        endpoint = endpointRepository.save(endpoint);
        WebhookResponse response = mapToResponse(endpoint);
        response.setSecreto(secret); // solo visible al crear
        return response;
    }

    @Transactional
    public WebhookResponse update(Long id, WebhookRequest request) {
        WebhookEndpoint endpoint = endpointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook no encontrado"));

        endpoint.setUrl(request.getUrl());
        endpoint.setEventos(request.getEventos() != null ? String.join(",", request.getEventos()) : "");
        if (request.getActivo() != null) {
            endpoint.setActivo(request.getActivo());
        }

        endpoint = endpointRepository.save(endpoint);
        return mapToResponse(endpoint);
    }

    @Transactional
    public void delete(Long id) {
        if (!endpointRepository.existsById(id)) {
            throw new ResourceNotFoundException("Webhook no encontrado");
        }
        endpointRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<WebhookResponse> listAll() {
        return endpointRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WebhookResponse getById(Long id) {
        WebhookEndpoint endpoint = endpointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook no encontrado"));
        return mapToResponse(endpoint);
    }

    @Transactional(readOnly = true)
    public List<WebhookDeliveryResponse> getDeliveries(Long webhookId) {
        return deliveryRepository.findByWebhookIdOrderByCreatedAtDesc(webhookId).stream()
                .map(this::mapDeliveryToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public WebhookDeliveryResponse sendTest(Long webhookId) {
        WebhookEndpoint endpoint = endpointRepository.findById(webhookId)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook no encontrado"));

        WebhookDelivery delivery = WebhookDelivery.builder()
                .webhookId(webhookId)
                .evento("TEST_EVENT")
                .payload("{\"evento\":\"TEST_EVENT\",\"mensaje\":\"Prueba de webhook\"}")
                .estado(WebhookDelivery.Estado.PENDIENTE)
                .build();
        delivery = deliveryRepository.save(delivery);
        return mapDeliveryToResponse(delivery);
    }

    private WebhookResponse mapToResponse(WebhookEndpoint endpoint) {
        List<String> eventosList = endpoint.getEventos() != null && !endpoint.getEventos().isBlank()
                ? List.of(endpoint.getEventos().split(","))
                : List.of();

        return WebhookResponse.builder()
                .id(endpoint.getId())
                .url(endpoint.getUrl())
                .eventos(eventosList)
                .activo(endpoint.getActivo())
                .createdAt(endpoint.getCreatedAt())
                .build();
    }

    private WebhookDeliveryResponse mapDeliveryToResponse(WebhookDelivery delivery) {
        return WebhookDeliveryResponse.builder()
                .id(delivery.getId())
                .webhookId(delivery.getWebhookId())
                .evento(delivery.getEvento())
                .payload(delivery.getPayload())
                .statusCode(delivery.getStatusCode())
                .intentos(delivery.getIntentos())
                .ultimoIntento(delivery.getUltimoIntento())
                .estado(delivery.getEstado().name())
                .createdAt(delivery.getCreatedAt())
                .build();
    }
}
