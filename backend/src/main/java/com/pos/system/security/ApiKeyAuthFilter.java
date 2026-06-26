package com.pos.system.security;

import com.pos.system.entity.ApiKey;
import com.pos.system.repository.ApiKeyRepository;
import com.pos.system.service.ApiKeyService;
import com.pos.system.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ApiKeyRepository apiKeyRepository;
    private final RateLimiterService rateLimiterService;
    private final ApiKeyService apiKeyService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String apiKeyHeader = request.getHeader("X-API-Key");
        if (apiKeyHeader == null || apiKeyHeader.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"X-API-Key header is required\"}");
            return;
        }

        // 1. Hash the provided API key
        String keyHash;
        try {
            keyHash = hashKey(apiKeyHeader);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Internal server error\"}");
            return;
        }

        // 2. Look up in database
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyHashAndActivoTrue(keyHash);
        if (apiKeyOpt.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid API key\"}");
            return;
        }

        ApiKey apiKey = apiKeyOpt.get();

        // 3. Check expiration
        if (apiKey.getExpiracion() != null && apiKey.getExpiracion().isBefore(LocalDateTime.now())) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"API key has expired\"}");
            return;
        }

        // 4. Check rate limit
        if (!rateLimiterService.tryConsume(apiKey.getId(), apiKey.getRateLimit())) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
            return;
        }

        // 5. Log usage
        apiKeyService.logUsage(apiKey.getId(), request.getRequestURI(), request.getMethod(), 200, request.getRemoteAddr());

        // 6. Set API key in request attribute for downstream usage
        request.setAttribute("apiKey", apiKey);

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip filter for docs and non-public paths
        return !path.startsWith("/public/v1/");
    }

    private String hashKey(String apiKey) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
