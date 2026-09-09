package com.achadosedevolvidos.auth.service;

import com.achadosedevolvidos.auth.dto.AuthResponse;
import com.achadosedevolvidos.auth.dto.ForgotPasswordRequest;
import com.achadosedevolvidos.auth.dto.LoginRequest;
import com.achadosedevolvidos.auth.dto.LogoutRequest;
import com.achadosedevolvidos.auth.dto.MessageResponse;
import com.achadosedevolvidos.auth.dto.RefreshRequest;
import com.achadosedevolvidos.auth.dto.RegisterRequest;
import com.achadosedevolvidos.auth.dto.ResetPasswordRequest;
import com.achadosedevolvidos.auth.model.PasswordResetToken;
import com.achadosedevolvidos.auth.model.RefreshToken;
import com.achadosedevolvidos.auth.repository.PasswordResetTokenRepository;
import com.achadosedevolvidos.auth.repository.RefreshTokenRepository;
import com.achadosedevolvidos.auth.util.SecureTokenGenerator;
import com.achadosedevolvidos.auth.util.TokenHasher;
import com.achadosedevolvidos.shared.exception.AppException;
import com.achadosedevolvidos.user.model.User;
import com.achadosedevolvidos.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

/**
 * Fluxo de autenticação "Bearer Token": cadastro, login e ciclo de vida do
 * refresh token (emissão/rotação/revogação) para o login local (e-mail/senha), mais
 * o fluxo de esqueci-minha-senha. Não possui nenhuma dependência do fluxo
 * OAuth2/Google.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    @Value("${app.jwt.access-expiration-ms}")
    private long accessExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Value("${app.password-reset.redirect-uri}")
    private String passwordResetRedirectUri;

    @Value("${app.password-reset.expiration-minutes}")
    private long passwordResetExpirationMinutes;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new AppException("Este e-mail já está cadastrado", HttpStatus.CONFLICT);
        }

        // createdAt não é setado aqui: BaseEntity cuida disso via @PrePersist.
        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(User.Role.USER)
                .provider(User.AuthProvider.LOCAL)
                .build();

        userRepository.save(user);
        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        if (user.getProvider() == User.AuthProvider.GOOGLE) {
            throw new AppException(
                    "Esta conta usa login via Google. Utilize essa opção para entrar.",
                    HttpStatus.CONFLICT
            );
        }

        // Lança BadCredentialsException se e-mail/senha não conferirem.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        return buildAuthResponse(user);
    }

    public AuthResponse refreshToken(RefreshRequest request) {
        String email;
        try {
            email = jwtService.extractUsername(request.refreshToken());
        } catch (JwtException | IllegalArgumentException e) {
            // Token mal formado ou assinado com outra chave: mesmo tratamento de
            // "inválido" dado abaixo a um token bem-formado porém expirado.
            throw new AppException("Refresh token inválido ou expirado", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("Usuário não encontrado", HttpStatus.UNAUTHORIZED));

        if (!jwtService.isTokenValid(request.refreshToken(), user)) {
            throw new AppException("Refresh token inválido ou expirado", HttpStatus.UNAUTHORIZED);
        }

        // Além da assinatura/expiração do JWT em si, o token precisa existir no
        // banco e não ter sido revogado (logout, rotação anterior ou reset de senha).
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(TokenHasher.hash(request.refreshToken()))
                .filter(RefreshToken::isValid)
                .orElseThrow(() -> new AppException("Refresh token inválido ou expirado", HttpStatus.UNAUTHORIZED));

        // Rotação: este token não pode ser reaproveitado numa próxima chamada,
        // mesmo que ainda não tivesse expirado.
        storedToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(storedToken);

        return buildAuthResponse(user);
    }

    /**
     * Sempre retorna sucesso (idempotente): não há valor de segurança em
     * diferenciar "token já estava revogado" de "acabou de ser revogado agora", e
     * isso evita que um retry/duplo-clique do frontend vire erro.
     */
    public MessageResponse logout(LogoutRequest request) {
        refreshTokenRepository.findByTokenHash(TokenHasher.hash(request.refreshToken()))
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> {
                    token.setRevokedAt(LocalDateTime.now());
                    refreshTokenRepository.save(token);
                });

        return new MessageResponse("Logout realizado com sucesso.");
    }

    /**
     * Nunca revela se o e-mail existe ou não na base — resposta idêntica em
     * qualquer caso (e-mail inexistente, conta só-Google, ou sucesso de fato).
     */
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email())
                .filter(user -> user.getProvider() != User.AuthProvider.GOOGLE)
                .ifPresent(this::issuePasswordResetToken);

        return new MessageResponse(
                "Se o e-mail informado estiver cadastrado, enviaremos instruções para redefinir a senha."
        );
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(TokenHasher.hash(request.token()))
                .filter(PasswordResetToken::isValid)
                .orElseThrow(() -> new AppException("Token de redefinição inválido ou expirado", HttpStatus.UNAUTHORIZED));

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(token);

        // Uma sessão vazada não deve sobreviver a uma redefinição de senha.
        refreshTokenRepository.revokeAllByUserId(user.getId());

        return new MessageResponse("Senha redefinida com sucesso.");
    }

    private void issuePasswordResetToken(User user) {
        String rawToken = SecureTokenGenerator.generate();

        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .tokenHash(TokenHasher.hash(rawToken))
                .expiresAt(LocalDateTime.now().plusMinutes(passwordResetExpirationMinutes))
                .build();
        passwordResetTokenRepository.save(token);

        String resetLink = passwordResetRedirectUri + "?token=" + rawToken;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateToken(
                user, Map.of("role", user.getRole().name()), accessExpirationMs
        );
        // "jti" garante que dois refresh tokens emitidos no mesmo milissegundo
        // (mesmo subject/issuedAt/expiration) nunca colidam no hash persistido em
        // refresh_tokens.token_hash — sem isso, geração do JWT é puramente
        // determinística e duas chamadas rápidas (ex.: register seguido de login)
        // produziriam o mesmo JWT byte a byte.
        String refreshToken = jwtService.generateToken(
                user, Map.of("jti", UUID.randomUUID().toString()), refreshExpirationMs
        );

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHasher.hash(refreshToken))
                .expiresAt(LocalDateTime.now().plus(refreshExpirationMs, ChronoUnit.MILLIS))
                .build());

        return new AuthResponse(accessToken, refreshToken, "Bearer", accessExpirationMs / 1000);
    }
}
