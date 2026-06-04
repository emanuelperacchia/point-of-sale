package com.pos.system.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public Long getCurrentUserId() {
        Authentication auth = getCurrentAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return userDetails.getId();
        }
        return null;
    }

    public String getCurrentUserEmail() {
        Authentication auth = getCurrentAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return userDetails.getEmail();
        }
        return null;
    }

    public boolean isAuthenticated() {
        Authentication auth = getCurrentAuthentication();
        return auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetailsImpl;
    }
}
