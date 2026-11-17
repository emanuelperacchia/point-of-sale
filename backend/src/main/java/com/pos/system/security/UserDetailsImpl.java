package com.pos.system.security;

import com.pos.system.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

@Getter
public class UserDetailsImpl implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;
    private final User user;

    public UserDetailsImpl(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.enabled = user.getActive();
        this.user = user;

        // Mapear roles como ROLE_* y permisos del rol como autoridades
        this.authorities = user.getRoles().stream()
                .flatMap(role -> {
                    // Agregar el rol como autoridad (ROLE_ADMIN, etc.)
                    java.util.stream.Stream<SimpleGrantedAuthority> roleAuth =
                            java.util.stream.Stream.of(new SimpleGrantedAuthority("ROLE_" + role.getName().name()));
                    // Agregar los permisos del rol como autoridades (COMPRAS_READ, ADMIN_ALL, etc.)
                    java.util.stream.Stream<SimpleGrantedAuthority> permAuth =
                            role.getPermissions().stream()
                                    .map(perm -> new SimpleGrantedAuthority(perm.getName().name()));
                    return java.util.stream.Stream.concat(roleAuth, permAuth);
                })
                .collect(Collectors.toList());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
