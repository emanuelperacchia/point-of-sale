package com.pos.system.security;

import com.pos.system.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    @Autowired
    private ApiKeyService apiKeyService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws jakarta.servlet.ServletException, java.io.IOException {

        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null) {
            // Aquí iría la lógica de autenticación por API Key
        }

        filterChain.doFilter(request, response);
    }
}
