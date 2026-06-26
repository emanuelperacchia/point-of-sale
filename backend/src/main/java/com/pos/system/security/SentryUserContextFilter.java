package com.pos.system.security;

import com.pos.system.config.BranchContextHolder;
import io.sentry.Sentry;
import io.sentry.protocol.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que agrega contexto de usuario y sucursal a Sentry antes de cada request,
 * para que los errores capturados incluyan información del usuario autenticado.
 */
@Component
@RequiredArgsConstructor
public class SentryUserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // Setear contexto de usuario en Sentry
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
                User user = new User();
                user.setId(String.valueOf(userDetails.getUser().getId()));
                user.setEmail(userDetails.getUser().getEmail());
                user.setUsername(userDetails.getUser().getFullName());
                Sentry.setUser(user);
                Sentry.setTag("userId", String.valueOf(userDetails.getUser().getId()));
            }

            // Agregar branchId del contexto
            Long branchId = BranchContextHolder.getBranchId();
            if (branchId != null) {
                Sentry.setTag("branchId", String.valueOf(branchId));
            }

            // Agregar información del request
            Sentry.setTag("endpoint", request.getRequestURI());
            Sentry.setTag("method", request.getMethod());

            filterChain.doFilter(request, response);
        } finally {
            // Limpiar contexto de Sentry después del request
            Sentry.setUser(null);
            Sentry.clearBreadcrumbs();
            Sentry.configureScope(scope -> scope.clear());
        }
    }
}
