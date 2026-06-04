package com.pos.system.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtro de rate limiting simple para prevenir fuerza bruta en /api/auth/login.
 *
 * Usa una ventana deslizante en memoria (ConcurrentHashMap).
 * Para producción con múltiples instancias, migrar a Redis.
 */
@Component
@Order(1)
public class RateLimitingFilter implements Filter {

    private final Map<String, SlidingWindow> attempts = new ConcurrentHashMap<>();

    @Value("${security.rate-limit.login-attempts:5}")
    private int maxAttempts;

    @Value("${security.rate-limit.lockout-duration:900000}")
    private long lockoutDurationMs;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI();

        // Solo limitamos POST /api/auth/login
        if (!path.equals("/api/auth/login") || !request.getMethod().equals("POST")) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        SlidingWindow window = attempts.computeIfAbsent(clientIp, k -> new SlidingWindow(maxAttempts, lockoutDurationMs));

        if (window.isBlocked()) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"message\":\"Demasiados intentos. Intente nuevamente en " + (lockoutDurationMs / 1000 / 60) + " minutos.\"}");
            return;
        }

        window.increment();

        try {
            chain.doFilter(request, response);
        } finally {
            // Si la autenticación fue exitosa (status 200), reseteamos el contador
            if (response.getStatus() == 200) {
                attempts.remove(clientIp);
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Ventana deslizante para rate limiting.
     * threadsafe: usa synchronized en los métodos de estado mutable.
     */
    static class SlidingWindow {
        private final int maxAttempts;
        private final long windowDurationMs;
        private final AtomicInteger counter = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        SlidingWindow(int maxAttempts, long windowDurationMs) {
            this.maxAttempts = maxAttempts;
            this.windowDurationMs = windowDurationMs;
        }

        synchronized boolean isBlocked() {
            resetIfExpired();
            return counter.get() >= maxAttempts;
        }

        synchronized void increment() {
            resetIfExpired();
            counter.incrementAndGet();
        }

        private void resetIfExpired() {
            long now = System.currentTimeMillis();
            if (now - windowStart > windowDurationMs) {
                counter.set(0);
                windowStart = now;
            }
        }
    }
}
