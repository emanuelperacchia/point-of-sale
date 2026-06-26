package com.pos.system.service;

import com.pos.system.dto.request.ApiKeyRequest;
import com.pos.system.dto.response.ApiKeyResponse;
import com.pos.system.dto.response.ApiKeyUsageResponse;
import com.pos.system.entity.ApiKey;
import com.pos.system.entity.ApiKeyUsageLog;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.ApiKeyRepository;
import com.pos.system.repository.ApiKeyUsageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyUsageLogRepository usageLogRepository;
    private final RateLimiterService rateLimiterService;

    /**
     * Genera una clave en formato pos_live_xxxxxxxxxxx
     */
    private String generateKeyValue() {
        SecureRandom rng = new SecureRandom();
        byte[] bytes = new byte[24];
        rng.nextBytes(bytes);
        return "pos_live_" + HexFormat.of().formatHex(bytes);
    }

    private String hashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error al hashear API Key", e);
        }
    }

    @Transactional
    public ApiKeyResponse create(ApiKeyRequest request, Long userId) {
        if (apiKeyRepository.existsByNombreAndActivoTrue(request.getNombre())) {
            throw new BadRequestException("Ya existe una API Key activa con ese nombre");
        }

        String rawKey = generateKeyValue();
        String keyHash = hashKey(rawKey);
        String keyPrefix = rawKey.substring(0, 14); // "pos_live_xxxx..."

        String permisos = request.getPermisos() != null
                ? String.join(",", request.getPermisos())
                : "";

        ApiKey apiKey = ApiKey.builder()
                .nombre(request.getNombre())
                .keyHash(keyHash)
                .keyPrefix(keyPrefix)
                .permisos(permisos)
                .rateLimit(request.getRateLimit() != null ? request.getRateLimit() : 60)
                .expiracion(request.getExpiracion())
                .creadoPor(userId)
                .activo(true)
                .build();

        apiKey = apiKeyRepository.save(apiKey);

        // Devolver la clave completa SOLO al crearla
        ApiKeyResponse response = mapToResponse(apiKey);
        response.setApiKey(rawKey);
        return response;
    }

    @Transactional(readOnly = true)
    public ApiKeyResponse getById(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("API Key no encontrada"));
        return mapToResponse(apiKey);
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> listAll() {
        return apiKeyRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ApiKeyResponse update(Long id, ApiKeyRequest request) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("API Key no encontrada"));

        if (request.getNombre() != null) {
            apiKey.setNombre(request.getNombre());
        }
        if (request.getPermisos() != null) {
            apiKey.setPermisos(String.join(",", request.getPermisos()));
        }
        if (request.getRateLimit() != null) {
            // Si cambia el rate limit, recrear el bucket
            rateLimiterService.removeBucket(id);
            apiKey.setRateLimit(request.getRateLimit());
        }
        if (request.getExpiracion() != null) {
            apiKey.setExpiracion(request.getExpiracion());
        }

        apiKey = apiKeyRepository.save(apiKey);
        return mapToResponse(apiKey);
    }

    @Transactional
    public void revoke(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("API Key no encontrada"));
        apiKey.setActivo(false);
        apiKeyRepository.save(apiKey);
        rateLimiterService.removeBucket(id);
        log.info("API Key {} revocada: {}", id, apiKey.getNombre());
    }

    @Transactional(readOnly = true)
    public ApiKeyUsageResponse getUsage(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("API Key no encontrada"));

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
        long requestsToday = usageLogRepository.countByApiKeyIdAndTimestampAfter(id, todayStart);
        long errors4xx = usageLogRepository.countErrorsSince(id, todayStart);

        return ApiKeyUsageResponse.builder()
                .totalRequests(apiKey.getTotalRequests())
                .requestsToday(requestsToday)
                .ultimoUso(apiKey.getUltimoUso())
                .errores4xx(errors4xx)
                .errores5xx(0L) // simplificado; podríamos contar 500s
                .build();
    }

    @Transactional
    public void logUsage(Long apiKeyId, String endpoint, String metodo, int statusCode, String ip) {
        ApiKeyUsageLog logEntry = ApiKeyUsageLog.builder()
                .apiKeyId(apiKeyId)
                .endpoint(endpoint)
                .metodo(metodo)
                .statusCode(statusCode)
                .ip(ip)
                .build();
        usageLogRepository.save(logEntry);

        // Actualizar contadores en la key
        apiKeyRepository.findById(apiKeyId).ifPresent(key -> {
            key.setTotalRequests(key.getTotalRequests() + 1);
            key.setUltimoUso(LocalDateTime.now());
            apiKeyRepository.save(key);
        });
    }

    private ApiKeyResponse mapToResponse(ApiKey apiKey) {
        List<String> permisosList = apiKey.getPermisos() != null && !apiKey.getPermisos().isBlank()
                ? List.of(apiKey.getPermisos().split(","))
                : List.of();

        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .nombre(apiKey.getNombre())
                .keyPrefix(apiKey.getKeyPrefix())
                .permisos(permisosList)
                .rateLimit(apiKey.getRateLimit())
                .expiracion(apiKey.getExpiracion())
                .activo(apiKey.getActivo())
                .ultimoUso(apiKey.getUltimoUso())
                .totalRequests(apiKey.getTotalRequests())
                .createdAt(apiKey.getCreatedAt())
                .build();
    }
}
