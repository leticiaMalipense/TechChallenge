package br.com.autoshop.config.security;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.io.Serial;

public class UserAuthenticated implements UserDetails {
    @Serial
    private static final long serialVersionUID = 1L;
    private final String email;
    private final String password;
    private final String role;

    public UserAuthenticated(br.com.autoshop.repository.model.UserEntity user) {
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.role = user.getRole() == null ? null : user.getRole().name();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }


    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public @NonNull String getUsername() {
        return email;
    }

}
