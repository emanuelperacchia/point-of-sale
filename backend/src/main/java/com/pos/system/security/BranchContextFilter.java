package com.pos.system.security;

import com.pos.system.config.BranchContextHolder;
import com.pos.system.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que extrae el branchId del JWT y lo setea en BranchContextHolder
 * para que los servicios filtren automáticamente por sucursal.
 *
 * Si el usuario es ADMIN (sin branchId en claims), BranchContextHolder queda
 * en null, lo que significa "ver todas las sucursales".
 */
@Component
@RequiredArgsConstructor
public class BranchContextFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                Long branchId = jwtService.extractBranchId(jwt);
                if (branchId != null) {
                    BranchContextHolder.setBranchId(branchId);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            BranchContextHolder.clear();
        }
    }
}
