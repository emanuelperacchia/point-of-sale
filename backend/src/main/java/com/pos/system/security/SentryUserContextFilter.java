package com.pos.system.security;

import com.pos.system.entity.system.User;
import com.pos.system.repository.system.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SentryUserContextFilter extends OncePerRequestFilter {

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws jakarta.servlet.ServletException, java.io.IOException {

        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (email != null && userRepository.findByEmail(email).isPresent()) {
            User user = userRepository.findByEmail(email).get();
            SecurityContextHolder.getContext().getAuthentication().setDetails(user);
        }

        filterChain.doFilter(request, response);
    }
}
