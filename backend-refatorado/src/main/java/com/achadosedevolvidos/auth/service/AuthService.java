package com.achadosedevolvidos.auth.service;

import com.achadosedevolvidos.auth.dto.AuthResponse;
import com.achadosedevolvidos.auth.dto.LoginRequest;
import com.achadosedevolvidos.auth.dto.RefreshRequest;
import com.achadosedevolvidos.auth.dto.RegisterRequest;
import com.achadosedevolvidos.shared.exception.AppException;
import com.achadosedevolvidos.user.model.User;
import com.achadosedevolvidos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Fluxo de autenticação "Bearer Token": cadastro e login local (e-mail/senha)
 * emitindo um JWT próprio. Não possui nenhuma dependência do fluxo OAuth2/Google.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${app.jwt.access-expiration-ms}")
    private long accessExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

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
        String email = jwtService.extractUsername(request.refreshToken());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("Usuário não encontrado", HttpStatus.UNAUTHORIZED));

        if (!jwtService.isTokenValid(request.refreshToken(), user)) {
            throw new AppException("Refresh token inválido ou expirado", HttpStatus.UNAUTHORIZED);
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateToken(
                user, Map.of("role", user.getRole().name()), accessExpirationMs
        );
        String refreshToken = jwtService.generateToken(user, Map.of(), refreshExpirationMs);

        return new AuthResponse(accessToken, refreshToken, "Bearer", accessExpirationMs / 1000);
    }
}
