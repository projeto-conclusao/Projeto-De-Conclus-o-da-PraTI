package com.achadosedevolvidos.user.model;

import java.util.UUID;

/**
 * Contrato comum implementado tanto por {@link User} (autenticação via Bearer JWT)
 * quanto por {@code CustomOAuth2User} (autenticação via OAuth2 Login/Google).
 *
 * <p>Isso permite que os Controllers dependam apenas de {@code @AuthenticationPrincipal
 * AuthenticatedUser currentUser}, sem precisar saber qual dos dois mecanismos, isolados
 * entre si, foi usado para autenticar a requisição.</p>
 */
public interface AuthenticatedUser {

    UUID getId();

    String getEmail();

    User.Role getRole();
}
