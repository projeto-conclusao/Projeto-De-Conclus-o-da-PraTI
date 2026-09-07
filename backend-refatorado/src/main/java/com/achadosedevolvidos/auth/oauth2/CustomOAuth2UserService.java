package com.achadosedevolvidos.auth.oauth2;

import com.achadosedevolvidos.user.model.User;
import com.achadosedevolvidos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * Mecanismo "OAuth 2.0": autentica via Google e faz find-or-create do usuário na
 * própria base. Não gera nem depende de JWT — a sessão HTTP (cookie) é o que mantém
 * o usuário autenticado a partir daqui, de forma isolada do JwtService.
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        if (email == null) {
            throw new OAuth2AuthenticationException("O provedor OAuth2 não retornou um e-mail");
        }

        User user = userRepository.findByEmail(email)
                .map(existing -> vincularGoogleSeNecessario(existing))
                .orElseGet(() -> criarUsuarioViaGoogle(email, name));

        return new CustomOAuth2User(user, oAuth2User.getAttributes());
    }

    private User vincularGoogleSeNecessario(User existing) {
        // Já existia uma conta local com este e-mail: passa a aceitar também o
        // login via Google, mantendo a senha local intacta (login por JWT continua ok).
        if (existing.getProvider() == User.AuthProvider.LOCAL) {
            existing.setProvider(User.AuthProvider.LOCAL_AND_GOOGLE);
            return userRepository.save(existing);
        }
        return existing;
    }

    private User criarUsuarioViaGoogle(String email, String name) {
        // createdAt não é setado aqui: BaseEntity cuida disso via @PrePersist.
        User novoUsuario = User.builder()
                .name(name != null ? name : email)
                .email(email)
                .password(null) // conta sem senha local: só entra via Google
                .role(User.Role.USER)
                .provider(User.AuthProvider.GOOGLE)
                .build();
        return userRepository.save(novoUsuario);
    }
}
