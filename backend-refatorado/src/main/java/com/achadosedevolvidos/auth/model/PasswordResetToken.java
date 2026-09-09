package com.achadosedevolvidos.auth.model;

import com.achadosedevolvidos.shared.model.BaseEntity;
import com.achadosedevolvidos.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Guarda apenas o hash do token de redefinição de senha — o valor bruto só existe
 * no e-mail enviado ao usuário, nunca no banco. Uso único: uma vez consumido
 * (usedAt setado), a linha nunca mais é aceita mesmo que ainda não tenha expirado.
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class PasswordResetToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    public boolean isValid() {
        return usedAt == null && expiresAt.isAfter(LocalDateTime.now());
    }
}
