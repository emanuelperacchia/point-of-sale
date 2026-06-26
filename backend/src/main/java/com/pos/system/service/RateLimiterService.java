package com.pos.system.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter basado en Bucket4j en memoria.
 * Cada API Key tiene su propio bucket con tokens que se recargan por minuto.
 */
@Service
public class RateLimiterService {

    private final Map<Long, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Obtiene o crea un bucket para la API Key.
     * Capacidad = rateLimit tokens, recarga completa cada 1 minuto.
     */
    public Bucket resolveBucket(Long apiKeyId, int rateLimit) {
        return buckets.computeIfAbsent(apiKeyId, key -> {
            Bandwidth limit = Bandwidth.classic(rateLimit, Refill.intervally(rateLimit, Duration.ofMinutes(1)));
            return Bucket.builder().addLimit(limit).build();
        });
    }

    /**
     * Intenta consumir 1 token. Retorna true si hay cupo, false si excede el límite.
     */
    public boolean tryConsume(Long apiKeyId, int rateLimit) {
        Bucket bucket = resolveBucket(apiKeyId, rateLimit);
        return bucket.tryConsume(1);
    }

    /**
     * Elimina el bucket cuando se revoca una API Key.
     */
    public void removeBucket(Long apiKeyId) {
        buckets.remove(apiKeyId);
    }
}
