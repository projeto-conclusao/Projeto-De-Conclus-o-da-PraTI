package com.achadosedevolvidos.auth.oauth2;

import com.achadosedevolvidos.user.model.AuthenticatedUser;
import com.achadosedevolvidos.user.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Adapta o {@link User} interno ao contrato {@link OAuth2User} do Spring Security,
 * e também implementa {@link AuthenticatedUser} — assim os Controllers podem usar
 * {@code @AuthenticationPrincipal AuthenticatedUser} sem saber se o login veio do
 * Google (OAuth2/sessão) ou do login local (Bearer JWT).
 */
public class CustomOAuth2User implements OAuth2User, AuthenticatedUser {

    private final User user;
    private final Map<String, Object> attributes;

    public CustomOAuth2User(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getAuthorities();
    }

    @Override
    public String getName() {
        return user.getEmail();
    }

    @Override
    public UUID getId() {
        return user.getId();
    }

    @Override
    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public User.Role getRole() {
        return user.getRole();
    }
}
