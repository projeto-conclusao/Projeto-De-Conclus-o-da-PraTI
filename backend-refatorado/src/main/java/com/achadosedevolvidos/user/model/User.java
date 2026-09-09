package com.achadosedevolvidos.user.model;

import com.achadosedevolvidos.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Entidade central de autenticação. É a mesma tabela/entidade usada pelos dois
 * mecanismos de login (local + JWT e Google + OAuth2), mas cada mecanismo lê/grava
 * essa entidade de forma independente — nenhum dos dois depende do outro em runtime.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class User extends BaseEntity implements UserDetails, AuthenticatedUser {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    /** Nulo para contas que só entram via Google (provider = GOOGLE). */
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    // --- Implementação de UserDetails (usada pelo fluxo de Bearer JWT) ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
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
        return true;
    }

    public enum Role { USER, ADMIN }

    /**
     * LOCAL_AND_GOOGLE cobre o caso de um usuário que se cadastrou com e-mail/senha
     * e depois também entrou com o Google usando o mesmo e-mail: ele mantém a senha
     * local (continua podendo logar via JWT) e passa a poder logar via Google também.
     */
    public enum AuthProvider { LOCAL, GOOGLE, LOCAL_AND_GOOGLE }
}
